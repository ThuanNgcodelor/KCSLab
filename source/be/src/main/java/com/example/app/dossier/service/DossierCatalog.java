package com.example.app.dossier.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.app.dossier.storage.ArchivePaths;
import com.example.app.dossier.storage.DossierStorageService;

@Service
public class DossierCatalog {
    private static final ZoneId VIETNAM=ZoneId.of("Asia/Ho_Chi_Minh");
    private final JdbcTemplate jdbc;
    private final DossierStorageService storage;
    public DossierCatalog(JdbcTemplate jdbc, DossierStorageService storage) { this.jdbc=jdbc; this.storage=storage; }
    public record Scope(Integer year, Integer month, Long receiptId, String q) {}
    public record Filter(String sql, List<Object> args) {}
    public record Page<T>(List<T> content, long totalElements, int page, int size) {}
    public record FileRow(String source, long id, long receiptId, long locationId, String relativePath,
            String storageName, String originalName, String contentType, long size, String checksum,
            String category, Long releaseId, Long sampleId, Integer version, boolean historical, Instant uploadedAt, String uploadedBy) {}
    public record Summary(long dossiers, long files, long bytes) {}
    public record Period(int year, int month, long dossiers) {}
    private static final String FILES = """
        SELECT 'RECEIPT' source,a.id,a.receipt_id,a.storage_location_id,a.relative_path,a.storage_name,
          a.original_name,a.content_type,a.size,a.checksum,a.category,NULL release_id,NULL sample_id,NULL version,
          0 historical,a.uploaded_at,u.display_name uploaded_by FROM receipt_attachments a
        LEFT JOIN users u ON u.id=a.uploaded_by_user_id
        UNION ALL
        SELECT 'RESULT',f.id,t.receipt_id,f.storage_location_id,f.relative_path,f.storage_name,
          f.original_name,f.content_type,f.size,f.checksum,f.kind,f.release_id,ti.sample_id,rr.version,
          (f.removed_at IS NOT NULL OR rr.status <> 'PUBLISHED'),f.uploaded_at,u.display_name
        FROM result_release_files f JOIN result_releases rr ON rr.id=f.release_id
        JOIN testing_records tr ON tr.id=rr.testing_record_id
        JOIN sample_transfer_items ti ON ti.id=tr.transfer_item_id JOIN sample_transfers t ON t.id=ti.transfer_id
        LEFT JOIN users u ON u.id=f.uploaded_by_user_id
        """;
    public Filter filter(Scope s) {
        if (s.year != null && (s.year < 2000 || s.year > 2200) || s.month != null && (s.year == null || s.month < 1 || s.month > 12)
                || s.receiptId != null && s.receiptId <= 0 || s.q != null && s.q.length()>120)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bộ lọc hồ sơ không hợp lệ");
        StringBuilder sql = new StringBuilder("1=1"); List<Object> args = new ArrayList<>();
        if (s.year != null) {
            sql.append(" AND r.received_at>=? AND r.received_at<?");
            args.add(start(s.year,s.month == null ? 1 : s.month));
            args.add(s.month == null || s.month == 12 ? start(s.year+1,1) : start(s.year,s.month+1));
        }
        if (s.receiptId != null) { sql.append(" AND r.id=?"); args.add(s.receiptId); }
        if (s.q != null && !s.q.isBlank()) { sql.append(" AND LOCATE(?,r.request_number)>0"); args.add(s.q.trim()); }
        return new Filter(sql.toString(),args);
    }
    private static final Map<String,String> SORTS=Map.of(
        "receivedAt,desc","r.received_at DESC,r.id DESC","receivedAt,asc","r.received_at,r.id",
        "requestNumber,asc","r.request_number,r.id","requestNumber,desc","r.request_number DESC,r.id DESC");
    public Page<Map<String,Object>> list(Scope s, int page, int size) { return list(s,"receivedAt,desc",page,size); }
    public Page<Map<String,Object>> list(Scope s, String sort, int page, int size) {
        pagination(page,size); Filter f=filter(s);
        String order=SORTS.get(sort);
        if(order==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Kiểu sắp xếp không hợp lệ");
        Long count=jdbc.queryForObject("SELECT COUNT(*) FROM receipts r WHERE "+f.sql,Long.class,f.args.toArray());
        List<Object> args=new ArrayList<>(f.args); args.add(size); args.add((long)page*size);
        List<Map<String,Object>> rows=jdbc.query("""
            SELECT r.id,r.request_number,r.receipt_code,r.received_at,r.sample_name,c.name customer_name,
              (SELECT COUNT(*) FROM samples s WHERE s.receipt_id=r.id) sample_count
            FROM receipts r JOIN customers c ON c.id=r.customer_id WHERE
            """+f.sql+" ORDER BY "+order+" LIMIT ? OFFSET ?",(r,n)->{
                Map<String,Object> row=new LinkedHashMap<>();
                row.put("id",r.getLong("id"));row.put("requestNumber",r.getString("request_number"));row.put("receiptCode",r.getString("receipt_code"));
                row.put("customerName",r.getString("customer_name"));row.put("sampleName",r.getString("sample_name"));
                row.put("receivedAt",r.getTimestamp("received_at").toInstant());row.put("sampleCount",r.getLong("sample_count"));
                row.put("fileCount",0L);row.put("totalBytes",0L);return row;
            },args.toArray());
        // Only the visible page is aggregated, so the cost does not grow with ten years of files.
        Map<Object,Map<String,Object>> byId=new HashMap<>();rows.forEach(row->byId.put(row.get("id"),row));
        if(!rows.isEmpty()) jdbc.query("SELECT receipt_id,COUNT(*),SUM(size) FROM ("+FILES+") f WHERE receipt_id IN ("
                +String.join(",",Collections.nCopies(rows.size(),"?"))+") GROUP BY receipt_id",(java.sql.ResultSet r)->{
            var row=byId.get(r.getLong(1));row.put("fileCount",r.getLong(2));row.put("totalBytes",r.getLong(3));
        },byId.keySet().toArray());
        return new Page<>(rows,count,page,size);
    }
    /** Totals of a whole filter (not of the visible page); also the estimate shown before an export. */
    public Summary summary(Scope s) {
        Filter f=filter(s);
        Long dossiers=jdbc.queryForObject("SELECT COUNT(*) FROM receipts r WHERE "+f.sql,Long.class,f.args.toArray());
        return jdbc.queryForObject("SELECT COUNT(*),COALESCE(SUM(f.size),0) FROM ("+FILES+") f JOIN receipts r ON r.id=f.receipt_id WHERE "+f.sql,
                (r,n)->new Summary(dossiers,r.getLong(1),r.getLong(2)),f.args.toArray());
    }
    /** Year/month tree by Vietnam receiving month, newest first; months without dossiers are omitted. */
    public List<Period> periods() {
        var range=jdbc.queryForObject("SELECT MIN(received_at),MAX(received_at) FROM receipts",(r,n)->new Timestamp[]{r.getTimestamp(1),r.getTimestamp(2)});
        List<Period> periods=new ArrayList<>();
        if(range[0]==null) return periods;
        int first=range[0].toInstant().atZone(VIETNAM).getYear(),last=range[1].toInstant().atZone(VIETNAM).getYear();
        for(int year=last;year>=first;year--) {
            List<Object> args=new ArrayList<>();
            for(int month=1;month<=12;month++) {args.add(start(year,month));args.add(month==12?start(year+1,1):start(year,month+1));}
            args.add(start(year,1));args.add(start(year+1,1));
            var counts=jdbc.queryForObject("SELECT "+String.join(",",Collections.nCopies(12,"COALESCE(SUM(received_at>=? AND received_at<?),0)"))
                +" FROM receipts WHERE received_at>=? AND received_at<?",(r,n)->{long[] c=new long[12];for(int i=0;i<12;i++)c[i]=r.getLong(i+1);return c;},args.toArray());
            for(int month=12;month>=1;month--) if(counts[month-1]>0) periods.add(new Period(year,month,counts[month-1]));
        }
        return periods;
    }
    private static Timestamp start(int year,int month) { return Timestamp.from(LocalDate.of(year,month,1).atStartOfDay(VIETNAM).toInstant()); }
    public Map<String,Object> receipt(long id) {
        var rows=jdbc.queryForList("SELECT * FROM receipts WHERE id=?",id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy hồ sơ");
        return rows.getFirst();
    }
    public String folder(Map<String,Object> receipt) {
        long id=((Number)receipt.get("id")).longValue();String number=(String)receipt.get("request_number");
        boolean owner=ArchivePaths.owns(number,(String)receipt.get("request_number_unique"));
        return receipt.get("archive_path") != null ? ArchivePaths.separate(receipt.get("archive_path").toString(),id,owner)
            : ArchivePaths.folder(id,number,owner,((Timestamp)receipt.get("received_at")).toInstant());
    }
    public String customerName(long receiptId) {
        return jdbc.queryForObject("SELECT c.name FROM receipts r JOIN customers c ON c.id=r.customer_id WHERE r.id=?",String.class,receiptId);
    }
    private static FileRow fileRow(ResultSet r) throws SQLException {
        return new FileRow(r.getString("source"),r.getLong("id"),r.getLong("receipt_id"),r.getLong("storage_location_id"),r.getString("relative_path"),
            r.getString("storage_name"),r.getString("original_name"),r.getString("content_type"),r.getLong("size"),r.getString("checksum"),
            r.getString("category"),r.getObject("release_id",Long.class),r.getObject("sample_id",Long.class),r.getObject("version",Integer.class),
            r.getBoolean("historical"),r.getTimestamp("uploaded_at").toInstant(),r.getString("uploaded_by"));
    }
    public Page<FileRow> files(long receiptId,int page,int size) {
        pagination(page,size); receipt(receiptId);
        Long count=jdbc.queryForObject("SELECT COUNT(*) FROM ("+FILES+") f WHERE receipt_id=?",Long.class,receiptId);
        return new Page<>(jdbc.query("SELECT * FROM ("+FILES+") f WHERE receipt_id=? ORDER BY historical,source,id LIMIT ? OFFSET ?",
                (r,n)->fileRow(r),receiptId,size,(long)page*size),count,page,size);
    }
    public FileRow file(long receiptId,String source,long id) {
        if (!Set.of("RECEIPT","RESULT").contains(source)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Loại file không hợp lệ");
        return jdbc.query("SELECT * FROM ("+FILES+") f WHERE receipt_id=? AND source=? AND id=?",(r,n)->fileRow(r),receiptId,source,id)
                .stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"File không thuộc hồ sơ"));
    }
    public java.nio.file.Path open(FileRow file) { return storage.read(file.locationId,file.relativePath,file.storageName); }
    public List<Map<String,Object>> receiptBatch(Scope scope,long afterId,Instant cutoff) {
        Filter f=filter(scope); var args=new ArrayList<>(f.args); args.add(afterId); args.add(Timestamp.from(cutoff));
        return jdbc.queryForList("SELECT r.* FROM receipts r WHERE "+f.sql+" AND r.id>? AND r.created_at<=? ORDER BY r.id LIMIT 50",args.toArray());
    }
    public List<FileRow> fileBatch(long receiptId,String source,long afterId,Instant cutoff) {
        return jdbc.query("SELECT * FROM ("+FILES+") f WHERE receipt_id=? AND source=? AND id>? AND uploaded_at<=? ORDER BY id LIMIT 100",
                (r,n)->fileRow(r),receiptId,source,afterId,Timestamp.from(cutoff));
    }
    public List<Map<String,Object>> audit(int page) {
        pagination(page,50); return jdbc.queryForList("SELECT * FROM dossier_audit ORDER BY id DESC LIMIT 50 OFFSET ?",(long)page*50);
    }
    private void pagination(int page,int size) {
        if (page<0 || page>1000000 || size<1 || size>100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Phân trang không hợp lệ");
    }
}
