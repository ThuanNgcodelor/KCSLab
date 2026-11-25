package com.example.app.dossier.service;

import com.example.app.dossier.storage.DossierStorageService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;

@Service
public class DossierExportJobs {
    final JdbcTemplate jdbc;
    final ObjectMapper json;
    final TransactionTemplate tx;
    private final DossierCatalog catalog;
    private final DossierStorageService storage;
    public DossierExportJobs(JdbcTemplate jdbc,ObjectMapper json,PlatformTransactionManager manager,DossierCatalog catalog,DossierStorageService storage) {
        this.jdbc=jdbc; this.json=json;this.tx=new TransactionTemplate(manager);this.catalog=catalog;this.storage=storage;
    }
    public Map<String,Object> create(DossierCatalog.Scope scope,String requestId,long actor) { return create(scope,false,requestId,actor); }
    public Map<String,Object> create(DossierCatalog.Scope scope,boolean allowMissing,String requestId,long actor) {
        catalog.filter(scope);
        return tx.execute(status->{
            jdbc.queryForList("SELECT id FROM dossier_export_worker WHERE id=1 FOR UPDATE");
            var old=jdbc.queryForList("SELECT id,scope_json,allow_missing FROM dossier_export_jobs WHERE actor_user_id=? AND request_id=?",actor,requestId);
            String encoded=json.writeValueAsString(scope);
            if (!old.isEmpty()) {
                if (!encoded.equals(old.getFirst().get("scope_json")) || allowMissing!=truthy(old.getFirst().get("allow_missing"))) throw error(409,"Mã yêu cầu đã dùng với bộ lọc khác");
                return detail(old.getFirst().get("id").toString());
            }
            Long pending=jdbc.queryForObject("SELECT COUNT(*) FROM dossier_export_jobs WHERE status IN ('QUEUED','PREPARING','PROCESSING','CANCEL_REQUESTED')",Long.class);
            if(pending>=10) throw error(429,"Hàng đợi đã đủ 10 lượt; chờ lượt đang chạy hoàn tất");
            Long own=jdbc.queryForObject("SELECT COUNT(*) FROM dossier_export_jobs WHERE actor_user_id=? AND status IN ('QUEUED','PREPARING','PROCESSING','CANCEL_REQUESTED')",Long.class,actor);
            if(own>=5) throw error(429,"Bạn đang có 5 lượt xuất chưa xong; chờ hoàn tất hoặc hủy bớt");
            String id=UUID.randomUUID().toString();var now=DossierStorageService.now();
            jdbc.update("INSERT INTO dossier_export_jobs(id,request_id,actor_user_id,scope_json,allow_missing,cutoff_at,created_at) VALUES (?,?,?,?,?,?,?)",id,requestId,actor,encoded,allowMissing,now,now);
            storage.audit(actor,"EXPORT_CREATE",id,null);
            return detail(id);
        });
    }
    public List<Map<String,Object>> list(int page) {
        if(page<0 || page>1000000) throw error(400,"Phân trang không hợp lệ");
        return jdbc.queryForList("SELECT id,status,scope_json,(SELECT COALESCE(r.request_number,r.receipt_code) FROM receipts r WHERE r.id=JSON_VALUE(scope_json,'$.receiptId' RETURNING UNSIGNED)) receipt_number,created_at,finished_at,expires_at,total_files,total_bytes,processed_files,processed_bytes,warning_count,allow_missing,error_code FROM dossier_export_jobs ORDER BY created_at DESC,id DESC LIMIT 20 OFFSET ?",page*20L);
    }
    public DossierCatalog.Page<Map<String,Object>> page(int page) {
        return new DossierCatalog.Page<>(list(page),jdbc.queryForObject("SELECT COUNT(*) FROM dossier_export_jobs",Long.class),page,20);
    }
    public Map<String,Object> detail(String id) {
        var rows=jdbc.queryForList("SELECT id,status,scope_json,created_at,cutoff_at,snapshot_at,finished_at,expires_at,total_files,total_bytes,processed_files,processed_bytes,warning_count,allow_missing,error_code FROM dossier_export_jobs WHERE id=?",id);
        if(rows.isEmpty()) throw error(404,"Không tìm thấy lượt xuất");
        var job=rows.getFirst();job.put("parts",jdbc.queryForList("SELECT part_number,size,checksum FROM dossier_export_parts WHERE job_id=? ORDER BY part_number",id));return job;
    }
    public void cancel(String id,long actor) {
        tx.executeWithoutResult(s->{
            if(jdbc.update("UPDATE dossier_export_jobs SET status=CASE WHEN status='QUEUED' THEN 'CANCELLED' ELSE 'CANCEL_REQUESTED' END WHERE id=? AND status IN ('QUEUED','PREPARING','PROCESSING')",id)!=1)
                throw error(409,"Lượt xuất không còn ở trạng thái có thể hủy");
            storage.audit(actor,"EXPORT_CANCEL",id,null);
        });
    }
    public record Claim(String id,String token,String attempt,DossierCatalog.Scope scope,boolean allowMissing,Instant cutoff) {}
    Claim claim() {
        return tx.execute(s->{
            String token=UUID.randomUUID().toString();
            if(jdbc.update("UPDATE dossier_export_worker SET lease_token=?,lease_until=DATE_ADD(CURRENT_TIMESTAMP(6),INTERVAL 5 MINUTE) WHERE id=1 AND (lease_until IS NULL OR lease_until<CURRENT_TIMESTAMP(6))",token)!=1) return null;
            var rows=jdbc.queryForList("SELECT * FROM dossier_export_jobs WHERE status='QUEUED' OR (status IN ('PREPARING','PROCESSING','CANCEL_REQUESTED') AND lease_until<CURRENT_TIMESTAMP(6)) ORDER BY created_at,id LIMIT 1 FOR UPDATE SKIP LOCKED");
            if(rows.isEmpty()) { release(token);return null; }
            var row=rows.getFirst();String id=row.get("id").toString();String attempt=UUID.randomUUID().toString();
            if("CANCEL_REQUESTED".equals(row.get("status"))) {
                jdbc.update("UPDATE dossier_export_jobs SET status='CANCELLED',finished_at=?,expires_at=?,lease_until=NULL WHERE id=?",DossierStorageService.now(),DossierStorageService.now(),id);release(token);return null;
            }
            // A job that keeps killing the process must not be retried forever.
            if(((Number)row.get("attempts")).intValue()>=3) {
                jdbc.update("UPDATE dossier_export_jobs SET status='FAILED',error_code='TOO_MANY_ATTEMPTS',finished_at=?,expires_at=?,lease_until=NULL WHERE id=?",DossierStorageService.now(),DossierStorageService.now(),id);release(token);return null;
            }
            jdbc.update("DELETE FROM dossier_export_entries WHERE job_id=?",id);
            jdbc.update("DELETE FROM dossier_export_parts WHERE job_id=?",id);
            jdbc.update("UPDATE dossier_export_jobs SET status='PREPARING',attempts=attempts+1,warning_count=0,attempt=?,lease_token=?,lease_until=DATE_ADD(CURRENT_TIMESTAMP(6),INTERVAL 5 MINUTE),snapshot_at=?,processed_files=0,processed_bytes=0,total_files=0,total_bytes=0,error_code=NULL WHERE id=?",attempt,token,DossierStorageService.now(),id);
            return new Claim(id,token,attempt,json.readValue(row.get("scope_json").toString(),DossierCatalog.Scope.class),truthy(row.get("allow_missing")),((java.sql.Timestamp)row.get("cutoff_at")).toInstant());
        });
    }
    boolean heartbeat(Claim c,long files,long bytes) {
        return Boolean.TRUE.equals(tx.execute(s->{
            if(jdbc.update("UPDATE dossier_export_worker SET lease_until=DATE_ADD(CURRENT_TIMESTAMP(6),INTERVAL 5 MINUTE) WHERE id=1 AND lease_token=? AND lease_until>CURRENT_TIMESTAMP(6)",c.token)!=1) return false;
            return jdbc.update("UPDATE dossier_export_jobs SET lease_until=DATE_ADD(CURRENT_TIMESTAMP(6),INTERVAL 5 MINUTE),processed_files=?,processed_bytes=? WHERE id=? AND lease_token=? AND status IN ('PREPARING','PROCESSING') AND lease_until>CURRENT_TIMESTAMP(6)",files,bytes,c.id,c.token)==1;
        }));
    }
    void finish(Claim c,String outcome,String error,long warnings) {
        Instant now=Instant.now();
        jdbc.update("UPDATE dossier_export_jobs SET status=CASE WHEN status='CANCEL_REQUESTED' THEN 'CANCELLED' ELSE ? END,error_code=?,warning_count=?,finished_at=?,expires_at=?,lease_until=NULL WHERE id=? AND lease_token=?",
            outcome,error,warnings,java.sql.Timestamp.from(now),java.sql.Timestamp.from(now.plus(7,java.time.temporal.ChronoUnit.DAYS)),c.id,c.token);
    }
    /** Planned shutdown: the job goes back to the queue and does not consume one of its attempts. */
    void requeue(Claim c) {
        jdbc.update("UPDATE dossier_export_jobs SET status='QUEUED',attempts=GREATEST(attempts-1,0),lease_token=NULL,lease_until=NULL WHERE id=? AND lease_token=? AND status IN ('PREPARING','PROCESSING')",c.id,c.token);
        finish(c,"CANCELLED",null,0);
    }
    static boolean truthy(Object value) { return value instanceof Boolean b ? b : value instanceof Number n && n.intValue()!=0; }
    void release(String token) { jdbc.update("UPDATE dossier_export_worker SET lease_token=NULL,lease_until=NULL WHERE id=1 AND lease_token=?",token); }
    static ResponseStatusException error(int status,String message) { return new ResponseStatusException(HttpStatus.valueOf(status),message); }
}
