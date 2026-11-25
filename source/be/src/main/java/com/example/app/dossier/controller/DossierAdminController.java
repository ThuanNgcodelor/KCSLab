package com.example.app.dossier.controller;

import com.example.app.dossier.service.DossierCatalog;
import com.example.app.dossier.storage.DossierStorageService;
import com.example.app.user.service.UserService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/dossiers")
@PreAuthorize("hasRole('ADMIN')")
public class DossierAdminController {
    private final DossierCatalog catalog;
    private final DossierStorageService storage;
    private final UserService users;
    public DossierAdminController(DossierCatalog catalog,DossierStorageService storage,UserService users) { this.catalog=catalog;this.storage=storage;this.users=users; }
    @GetMapping
    public DossierCatalog.Page<Map<String,Object>> list(@RequestParam(required=false) Integer year,@RequestParam(required=false) Integer month,
            @RequestParam(required=false) Long receiptId,@RequestParam(required=false) String q,
            @RequestParam(defaultValue="receivedAt,desc") String sort,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return catalog.list(new DossierCatalog.Scope(year,month,receiptId,q),sort,page,size);
    }
    @GetMapping("/summary")
    public DossierCatalog.Summary summary(@RequestParam(required=false) Integer year,@RequestParam(required=false) Integer month,
            @RequestParam(required=false) Long receiptId,@RequestParam(required=false) String q) {
        return catalog.summary(new DossierCatalog.Scope(year,month,receiptId,q));
    }
    @GetMapping("/periods")
    public List<DossierCatalog.Period> periods() { return catalog.periods(); }
    /** Physical paths and storage ids stay on the server. */
    public record FileView(String source,long id,String originalName,String contentType,long size,String checksum,String category,
            Long sampleId,Integer version,boolean historical,Instant uploadedAt,String uploadedBy) {}
    public record Detail(long id,String requestNumber,String receiptCode,String customerName,String sampleName,Instant receivedAt,String folder,DossierCatalog.Page<FileView> files) {}
    @GetMapping("/{id}")
    public Detail detail(@PathVariable long id,@RequestParam(defaultValue="0") int page) {
        var r=catalog.receipt(id); var files=catalog.files(id,page,50);
        return new Detail(id,(String)r.get("request_number"),(String)r.get("receipt_code"),catalog.customerName(id),(String)r.get("sample_name"),
            ((java.sql.Timestamp)r.get("received_at")).toInstant(),catalog.folder(r),
            new DossierCatalog.Page<>(files.content().stream().map(f->new FileView(f.source(),f.id(),f.originalName(),f.contentType(),f.size(),f.checksum(),
                f.category(),f.sampleId(),f.version(),f.historical(),f.uploadedAt(),f.uploadedBy())).toList(),files.totalElements(),page,50));
    }
    @GetMapping("/{id}/files/{source}/{fileId}")
    public ResponseEntity<FileSystemResource> content(@PathVariable long id,@PathVariable String source,@PathVariable long fileId,
            @RequestParam(defaultValue="false") boolean preview,@AuthenticationPrincipal Jwt jwt) {
        var file=catalog.file(id,source,fileId); var path=catalog.open(file);
        // Thumbnails on the dossier page are viewing, not a download; auditing each one would bury real downloads.
        boolean image=file.contentType()!=null && file.contentType().startsWith("image/");
        if(!(preview && image)) storage.audit(users.getByEmail(jwt.getSubject()).getId(),"DOSSIER_DOWNLOAD",source+":"+fileId,"receipt="+id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff")
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.originalName(),StandardCharsets.UTF_8).build().toString())
            .contentType(mediaType(file.contentType())).body(new FileSystemResource(path));
    }
    private static MediaType mediaType(String value) {
        try { return MediaType.parseMediaType(value); } catch (InvalidMediaTypeException e) { return MediaType.APPLICATION_OCTET_STREAM; }
    }
    @GetMapping("/audit")
    public List<Map<String,Object>> audit(@RequestParam(defaultValue="0") int page) { return catalog.audit(page); }
}
