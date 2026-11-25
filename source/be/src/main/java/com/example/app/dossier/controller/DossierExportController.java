package com.example.app.dossier.controller;

import com.example.app.dossier.service.*;
import com.example.app.dossier.storage.DossierStorageService;
import com.example.app.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.nio.file.*;
import java.nio.channels.Channels;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/dossier-exports")
@PreAuthorize("hasRole('ADMIN')")
public class DossierExportController {
    private final DossierExportJobs jobs;
    private final DossierExportWorker worker;
    private final DossierStorageService storage;
    private final UserService users;
    private final JdbcTemplate jdbc;
    public DossierExportController(DossierExportJobs jobs,DossierExportWorker worker,DossierStorageService storage,UserService users,JdbcTemplate jdbc) {
        this.jobs=jobs;this.worker=worker;this.storage=storage;this.users=users;this.jdbc=jdbc;
    }
    public record Create(Integer year,Integer month,Long receiptId,@Size(max=120) String q,Boolean allowMissing,@NotBlank @Size(max=80) String requestId) {}
    @PostMapping
    public ResponseEntity<Map<String,Object>> create(@Valid @RequestBody Create r,@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(jobs.create(new DossierCatalog.Scope(r.year,r.month,r.receiptId,r.q),Boolean.TRUE.equals(r.allowMissing),r.requestId,users.getByEmail(jwt.getSubject()).getId()));
    }
    @GetMapping
    public DossierCatalog.Page<Map<String,Object>> list(@RequestParam(defaultValue="0") int page) {return jobs.page(page);}
    @GetMapping("/{id}")
    public Map<String,Object> detail(@PathVariable String id) {return jobs.detail(id);}
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable String id,@AuthenticationPrincipal Jwt jwt) {
        jobs.cancel(id,users.getByEmail(jwt.getSubject()).getId());return ResponseEntity.noContent().build();
    }
    @GetMapping("/{id}/parts/{part}")
    public void download(@PathVariable String id,@PathVariable int part,@RequestHeader(value="Range",required=false) String range,
            @AuthenticationPrincipal Jwt jwt,HttpServletResponse response) throws IOException {
        var rows=jdbc.queryForList("SELECT p.* FROM dossier_export_parts p JOIN dossier_export_jobs j ON j.id=p.job_id WHERE p.job_id=? AND p.part_number=? AND j.status IN ('COMPLETED','COMPLETED_WITH_WARNINGS') AND j.expires_at>?",id,part,DossierStorageService.now());
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Gói xuất chưa sẵn sàng hoặc đã hết hạn");
        var row=rows.getFirst();Path path;
        try {path=worker.partPath(row.get("relative_path").toString());}
        catch(IOException e) {throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Gói xuất tạm không truy cập được");}
        long length=Files.size(path),start=0,end=length-1;
        if(range!=null) {
            if(!range.matches("bytes=[0-9]+-[0-9]*")) {response.setHeader("Content-Range","bytes */"+length);response.sendError(416);return;}
            try {String[] bounds=range.substring(6).split("-",-1);start=Long.parseLong(bounds[0]);if(!bounds[1].isEmpty())end=Math.min(end,Long.parseLong(bounds[1]));}
            catch(NumberFormatException e){response.setHeader("Content-Range","bytes */"+length);response.sendError(416);return;}
            if(start>end || start>=length) {response.setHeader("Content-Range","bytes */"+length);response.sendError(416);return;}
        }
        if(jdbc.update("UPDATE dossier_export_jobs SET download_until=DATE_ADD(CURRENT_TIMESTAMP(6),INTERVAL 5 MINUTE) WHERE id=? AND status IN ('COMPLETED','COMPLETED_WITH_WARNINGS') AND expires_at>?",id,DossierStorageService.now())!=1)
            throw new ResponseStatusException(HttpStatus.GONE,"Gói xuất đã hết hạn");
        storage.audit(users.getByEmail(jwt.getSubject()).getId(),"EXPORT_DOWNLOAD",id,"part="+part);
        response.setStatus(range==null?200:206);response.setContentType("application/zip");response.setHeader("Cache-Control","no-store");
        response.setHeader("X-Content-Type-Options","nosniff");response.setHeader("Accept-Ranges","bytes");
        // Multi-gigabyte parts must flow through nginx instead of being spooled to its temp directory.
        response.setHeader("X-Accel-Buffering","no");
        response.setHeader("Content-Disposition","attachment; filename=\"ho-so-"+id+"-"+part+".zip\"");
        response.setHeader("ETag","\""+row.get("checksum")+"\"");response.setContentLengthLong(end-start+1);
        if(range!=null) response.setHeader("Content-Range","bytes "+start+"-"+end+"/"+length);
        // Synchronous streaming avoids async request timeouts; the browser writes to its chosen download location.
        try(var channel=Files.newByteChannel(path,StandardOpenOption.READ)) {
            channel.position(start);InputStream in=Channels.newInputStream(channel);OutputStream out=response.getOutputStream();
            long remaining=end-start+1,beat=System.nanoTime();byte[] buffer=new byte[65536];
            while(remaining>0) {
                int n=in.read(buffer,0,(int)Math.min(buffer.length,remaining));if(n<0) throw new EOFException();
                out.write(buffer,0,n);remaining-=n;
                if(System.nanoTime()-beat>30_000_000_000L) {jdbc.update("UPDATE dossier_export_jobs SET download_until=DATE_ADD(CURRENT_TIMESTAMP(6),INTERVAL 5 MINUTE) WHERE id=? AND status IN ('COMPLETED','COMPLETED_WITH_WARNINGS')",id);beat=System.nanoTime();}
            }
        }
    }
}
