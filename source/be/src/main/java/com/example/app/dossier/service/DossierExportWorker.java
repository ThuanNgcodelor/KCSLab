package com.example.app.dossier.service;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.app.dossier.storage.DossierStorageService;

@Service
public class DossierExportWorker {
    private static final Logger log=LoggerFactory.getLogger(DossierExportWorker.class);
    private final ExecutorService executor=Executors.newSingleThreadExecutor(Thread.ofPlatform().name("dossier-export").daemon().factory());
    private final AtomicBoolean busy=new AtomicBoolean();
    private volatile boolean stopping;
    private final DossierExportJobs jobs;
    private final DossierCatalog catalog;
    private final DossierStorageService storage;
    private final JdbcTemplate jdbc;
    private final Path root;
    private final boolean enabled;
    private final long partBytes, reserve, maxJobBytes;
    private final int partEntries;
    public DossierExportWorker(DossierExportJobs jobs,DossierCatalog catalog,DossierStorageService storage,
            @Value("${app.dossier-exports.root:storage/exports}") String root,
            @Value("${app.dossier-exports.worker-enabled:true}") boolean enabled,
            @Value("${app.dossier-exports.part-bytes:2147483648}") long partBytes,
            @Value("${app.dossier-exports.part-entries:5000}") int partEntries,
            @Value("${app.dossier-exports.reserve-bytes:1073741824}") long reserve,
            @Value("${app.dossier-exports.max-job-bytes:53687091200}") long maxJobBytes) {
        this.jobs=jobs;this.jdbc=jobs.jdbc;this.catalog=catalog;this.storage=storage;
        this.root=Path.of(root).toAbsolutePath().normalize();this.enabled=enabled;
        this.partBytes=Math.max(1,partBytes);this.partEntries=Math.max(2,Math.min(5000,partEntries));
        this.reserve=Math.max(0,reserve);this.maxJobBytes=Math.max(1,maxJobBytes);
    }
    /** Exports own their thread: the shared scheduler also runs customer notifications and token refresh. */
    @Scheduled(fixedDelayString="${app.dossier-exports.worker-delay-ms:5000}")
    public void tick() {
        if(!enabled || stopping || !busy.compareAndSet(false,true)) return;
        try { executor.submit(()->{ try { runNext(); } catch(RuntimeException e) { log.warn("Dossier export worker: {}",e.getClass().getSimpleName()); } finally { busy.set(false); } }); }
        catch(RejectedExecutionException e) { busy.set(false); }
    }
    @PreDestroy
    void stop() throws InterruptedException {
        stopping=true;executor.shutdown();executor.awaitTermination(20,TimeUnit.SECONDS);
    }
    public void runNext() {
        var claim=jobs.claim();if(claim==null) return;
        Path attempt=root.resolve(claim.id()).resolve(claim.attempt());
        try {
            ensureRoot();deleteTree(root.resolve(claim.id()));Files.createDirectories(attempt);
            prepare(claim);
            pulse(claim,0,0);
            jdbc.update("UPDATE dossier_export_jobs SET status='PROCESSING' WHERE id=? AND lease_token=? AND status='PREPARING'",claim.id(),claim.token());
            long warnings=pack(claim,attempt);
            jobs.finish(claim,warnings>0?"COMPLETED_WITH_WARNINGS":"COMPLETED",null,warnings);
        } catch (Stopped e) { deleteTree(attempt);jobs.requeue(claim); }
        catch (Exception e) {
            // Keep customer names, paths and document content out of logs/errors.
            jobs.finish(claim,"FAILED",e instanceof Failure f ? f.code : "EXPORT_CHECK_FAILED",0);
            deleteTree(attempt);
        } finally { jobs.release(claim.token()); }
    }
    static final class Failure extends RuntimeException {
        final String code;
        Failure(String code) { super(code);this.code=code; }
    }
    static final class Stopped extends RuntimeException {}
    private void ensureRoot() throws IOException {
        // Custom export mounts must already exist. Never manufacture a missing NAS mount.
        if(root.equals(Path.of("storage/exports").toAbsolutePath().normalize())) Files.createDirectories(root);
        if(!Files.isDirectory(root)) throw new Failure("EXPORT_ROOT_UNAVAILABLE");
        if(Files.getFileStore(root).getUsableSpace()<reserve) throw new Failure("DISK_RESERVE");
    }
    private void pulse(DossierExportJobs.Claim c,long files,long bytes) throws IOException {
        if(stopping) throw new Stopped();
        if(!jobs.heartbeat(c,files,bytes)) throw new Failure("INTERRUPTED_OR_LEASE_LOST");
        if(Files.getFileStore(root).getUsableSpace()<reserve) throw new Failure("DISK_RESERVE");
    }
    private void prepare(DossierExportJobs.Claim c) throws IOException {
        long after=0;
        while(true) {
            var receipts=catalog.receiptBatch(c.scope(),after,c.cutoff());
            if(receipts.isEmpty()) break;
            for(var receipt:receipts) {
                long id=((Number)receipt.get("id")).longValue();String folder=catalog.folder(receipt);
                // A short repeatable-read snapshot per dossier; file eligibility is bounded by request cutoff.
                jobs.tx.executeWithoutResult(s->{
                    var current=catalog.receipt(id);
                    StringBuilder listing=new StringBuilder("\ufeffloai,id,ten_goc,duong_dan,so_byte,sha256,ngay_tai,nguoi_tai,lich_su\n");
                    addJson(c,folder+"/ho-so.json",Map.of("receipt",current,"fileCutoff",c.cutoff().toString(),
                        "snapshotNote","Dữ liệu nghiệp vụ được chụp khi chuẩn bị hồ sơ; file tải sau cutoff không nằm trong lượt xuất."));
                    for(String source:List.of("RECEIPT","RESULT")) {
                        long last=0;
                        while(true) {
                            var files=catalog.fileBatch(id,source,last,c.cutoff());if(files.isEmpty()) break;
                            for(var file:files) {
                                String branch=source.equals("RECEIPT") ? ("TRANSFER_EVIDENCE".equals(file.category())?"02-ban-giao/":"01-tiep-nhan/")
                                    : "03-ket-qua/mau-"+file.sampleId()+"/phien-ban-"+file.version()+"-r"+file.releaseId()+"/";
                                if(file.historical()) branch="04-lich-su/"+branch;
                                String path=folder+"/"+branch+source.toLowerCase(Locale.ROOT)+"-"+file.id()+"-"+safeName(file.originalName());
                                jdbc.update("INSERT INTO dossier_export_entries(job_id,entry_path,location_id,relative_path,storage_name,size,checksum) VALUES (?,?,?,?,?,?,?)",
                                    c.id(),path,file.locationId(),file.relativePath(),file.storageName(),file.size(),file.checksum());
                                listing.append(csv(source+":"+file.category())).append(',').append(file.id()).append(',').append(csv(file.originalName())).append(',')
                                    .append(csv(path)).append(',').append(file.size()).append(',').append(file.checksum()).append(',').append(file.uploadedAt()).append(',')
                                    .append(csv(file.uploadedBy()==null?"":file.uploadedBy())).append(',').append(file.historical()?"co":"khong").append('\n');
                                last=file.id();
                            }
                        }
                    }
                    addText(c,folder+"/danh-muc-file.csv",listing.toString());
                    snapshotTables(c,folder,id);
                });
                pulse(c,0,0);after=id;
            }
        }
        var total=jdbc.queryForMap("SELECT COUNT(*) files,COALESCE(SUM(size),0) bytes FROM dossier_export_entries WHERE job_id=?",c.id());
        long bytes=((Number)total.get("bytes")).longValue();
        if(bytes>maxJobBytes) throw new Failure("QUOTA_EXCEEDED");
        if(Files.getFileStore(root).getUsableSpace()-reserve<bytes+bytes/50+1048576) throw new Failure("DISK_RESERVE");
        jdbc.update("UPDATE dossier_export_jobs SET total_files=?,total_bytes=? WHERE id=? AND lease_token=?",total.get("files"),bytes,c.id(),c.token());
    }
    /** The customer lookup code opens the public page like a password; server file locations are internal. */
    private static final List<String> HIDDEN=List.of("lookup_code","storage_location_id","relative_path","storage_name","request_number_unique");
    // Only business tables are allowed. No users, sessions, credentials or integration settings.
    private void snapshotTables(DossierExportJobs.Claim c,String folder,long receiptId) {
        Map<String,String> tables=new LinkedHashMap<>();
        tables.put("customers","x.id IN (SELECT customer_id FROM receipts WHERE id=?)");
        tables.put("samples","x.receipt_id=?");
        tables.put("sample_code_events","x.sample_id IN (SELECT id FROM samples WHERE receipt_id=?)");
        tables.put("receipt_attachments","x.receipt_id=?");
        tables.put("test_requests","x.id IN (SELECT test_request_id FROM receipts WHERE id=?)");
        tables.put("test_request_items","x.test_request_id IN (SELECT test_request_id FROM receipts WHERE id=?)");
        tables.put("sampling_plans","x.test_request_id IN (SELECT test_request_id FROM receipts WHERE id=?)");
        tables.put("field_sampling_forms","x.id IN (SELECT field_sampling_id FROM receipts WHERE id=?)");
        tables.put("field_sampling_items","x.field_sampling_id IN (SELECT field_sampling_id FROM receipts WHERE id=?)");
        tables.put("sample_transfers","x.receipt_id=?");
        String transfers="SELECT id FROM sample_transfers WHERE receipt_id=?";
        tables.put("sample_transfer_items","x.transfer_id IN ("+transfers+")");
        String records="SELECT tr.id FROM testing_records tr JOIN sample_transfer_items ti ON ti.id=tr.transfer_item_id WHERE ti.transfer_id IN ("+transfers+")";
        tables.put("testing_records","x.id IN ("+records+")");
        for(String t:List.of("testing_equipment","testing_reagents","testing_measurements")) tables.put(t,"x.testing_record_id IN ("+records+")");
        String releases="SELECT id FROM result_releases WHERE testing_record_id IN ("+records+")";
        tables.put("result_releases","x.id IN ("+releases+")");
        for(String t:List.of("result_release_files","result_release_items","result_release_events")) tables.put(t,"x.release_id IN ("+releases+")");
        for(var table:tables.entrySet()) {
            long after=0;
            while(true) {
                var rows=jdbc.queryForList("SELECT x.* FROM "+table.getKey()+" x WHERE "+table.getValue()+" AND x.id>? ORDER BY x.id LIMIT 100",receiptId,after);
                if(rows.isEmpty()) break;
                for(var row:rows) {
                    after=((Number)row.get("id")).longValue();
                    HIDDEN.forEach(row::remove);
                    addJson(c,folder+"/05-du-lieu/"+table.getKey()+"/"+after+".json",row);
                }
                if(stopping) throw new Stopped();
                if(!jobs.heartbeat(c,0,0)) throw new Failure("INTERRUPTED_OR_LEASE_LOST");
            }
        }
    }
    private void addJson(DossierExportJobs.Claim c,String path,Object row) { addText(c,path,jobs.json.writeValueAsString(row)); }
    private void addText(DossierExportJobs.Claim c,String path,String payload) {
        byte[] data=payload.getBytes(StandardCharsets.UTF_8);
        jdbc.update("INSERT INTO dossier_export_entries(job_id,entry_path,size,checksum,payload) VALUES (?,?,?,?,?)",c.id(),path,data.length,hash(data),payload);
    }
    static String safeName(String name) {
        String safe=name.replaceAll("[^\\p{L}\\p{N}._ -]","_").replaceAll("[. ]+$","");
        return safe.isBlank()?"file":safe.substring(0,Math.min(140,safe.length()));
    }
    /** @return number of files that are missing or differ from their recorded checksum (only when the Admin allowed it). */
    private long pack(DossierExportJobs.Claim c,Path dir) throws IOException {
        long after=0,files=0,bytes=0,warnings=0;int part=0;
        Part output=null;
        try {
            while(true) {
                var rows=jdbc.queryForList("SELECT * FROM dossier_export_entries WHERE job_id=? AND id>? ORDER BY id LIMIT 100",c.id(),after);
                if(rows.isEmpty()) break;
                for(var row:rows) {
                    long size=((Number)row.get("size")).longValue();
                    if(output==null || output.count>=partEntries-1 || (output.count>0 && output.bytes+size>partBytes)) {
                        if(output!=null) output.finish(c,files,bytes);
                        output=new Part(dir,++part);
                    }
                    pulse(c,files,bytes);
                    String entry=row.get("entry_path").toString(),expected=row.get("checksum").toString();
                    if(entry.startsWith("/") || entry.contains("\\") || Arrays.asList(entry.split("/")).contains("..")) throw new Failure("EXPORT_CHECK_FAILED");
                    InputStream source;
                    try { source=open(row); }
                    catch(Failure e) {
                        if(!c.allowMissing()) throw e;
                        output.manifest.write(csv(entry)+","+size+","+expected+",THIEU_FILE\n");
                        warnings++;files++;after=((Number)row.get("id")).longValue();continue;
                    }
                    MessageDigest digest=digest();long copied=0,beat=System.nanoTime();boolean readError=false;
                    output.zip.putNextEntry(new ZipEntry(entry));
                    try(InputStream in=source) {
                        byte[] buffer=new byte[65536];
                        while(true) {
                            int n;
                            try { n=in.read(buffer); } catch(IOException e) { readError=true;break; }
                            if(n<0) break;
                            copied+=n;
                            if(copied>size && !c.allowMissing()) throw new Failure("SOURCE_MODIFIED");
                            digest.update(buffer,0,n);output.zip.write(buffer,0,n);
                            if(stopping) throw new Stopped();
                            if(System.nanoTime()-beat>10_000_000_000L) { pulse(c,files,bytes+copied);beat=System.nanoTime(); }
                        }
                    }
                    String checksum=HexFormat.of().formatHex(digest.digest());
                    String state=readError ? "DOC_DO_DANG" : copied!=size || !checksum.equalsIgnoreCase(expected) ? "KHAC_CHECKSUM" : "DAT";
                    if(!state.equals("DAT")) {
                        if(!c.allowMissing()) throw new Failure(readError?"SOURCE_UNAVAILABLE":"SOURCE_MODIFIED");
                        warnings++;
                    }
                    output.zip.closeEntry();output.manifest.write(csv(entry)+","+copied+","+checksum+","+state+"\n");
                    output.count++;output.bytes+=copied;bytes+=copied;files++;
                    after=((Number)row.get("id")).longValue();
                }
            }
            // Even an empty selection produces an explicit, usable manifest.
            if(output==null) output=new Part(dir,++part);
            output.finish(c,files,bytes);output=null;pulse(c,files,bytes);
            return warnings;
        } finally { if(output!=null) output.close(); }
    }
    private InputStream open(Map<String,Object> row) {
        if(row.get("payload")!=null) return new ByteArrayInputStream(row.get("payload").toString().getBytes(StandardCharsets.UTF_8));
        try { return Files.newInputStream(storage.read(((Number)row.get("location_id")).longValue(),(String)row.get("relative_path"),(String)row.get("storage_name"))); }
        catch(IOException | ResponseStatusException e) { throw new Failure("SOURCE_UNAVAILABLE"); }
    }
    private final class Part implements Closeable {
        final Path partial,manifestPath,complete;
        final int number;
        final ZipOutputStream zip;
        final BufferedWriter manifest;
        int count;long bytes;
        Part(Path dir,int number) throws IOException {
            this.number=number;this.complete=dir.resolve("ho-so-"+number+".zip");this.partial=dir.resolve("ho-so-"+number+".partial");this.manifestPath=dir.resolve("danh-muc-"+number+".csv");
            zip=new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(partial,StandardOpenOption.CREATE_NEW)));
            manifest=Files.newBufferedWriter(manifestPath,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
            manifest.write("\ufeffduong_dan,so_byte,sha256,trang_thai\n");
        }
        void finish(DossierExportJobs.Claim c,long files,long bytes) throws IOException {
            manifest.close();zip.putNextEntry(new ZipEntry("danh-muc-goi-"+number+".csv"));
            try(InputStream in=Files.newInputStream(manifestPath)) { in.transferTo(zip); }
            zip.closeEntry();zip.close();
            Files.move(partial,complete,StandardCopyOption.ATOMIC_MOVE);Files.delete(manifestPath);
            MessageDigest digest=digest();byte[] buffer=new byte[65536];long beat=System.nanoTime();
            try(InputStream in=Files.newInputStream(complete)) {
                int n;while((n=in.read(buffer))!=-1) {digest.update(buffer,0,n);if(System.nanoTime()-beat>10_000_000_000L){pulse(c,files,bytes);beat=System.nanoTime();}}
            }
            pulse(c,files,bytes);
            jdbc.update("INSERT INTO dossier_export_parts(job_id,part_number,relative_path,size,checksum) VALUES (?,?,?,?,?)",c.id(),number,
                root.relativize(complete).toString().replace('\\','/'),Files.size(complete),HexFormat.of().formatHex(digest.digest()));
        }
        @Override public void close() throws IOException { try {manifest.close();} finally {zip.close();} }
    }
    public Path partPath(String relative) throws IOException {
        Path p=root.resolve(relative).normalize();
        if(!p.startsWith(root) || !Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS) || !p.toRealPath().startsWith(root.toRealPath())) throw new IOException("Part unavailable");return p;
    }
    private void deleteTree(Path dir) {
        if(!dir.normalize().startsWith(root) || dir.normalize().equals(root) || !Files.isDirectory(dir,LinkOption.NOFOLLOW_LINKS)) return;
        try(var walk=Files.walk(dir)) { walk.sorted(Comparator.reverseOrder()).forEach(p->{try {Files.deleteIfExists(p);} catch(IOException ignored){}}); }
        catch(IOException ignored) {}
    }
    /** Removes expired packages only. Entries are deleted last, so an interrupted cleanup is picked up again. */
    @Scheduled(fixedDelayString="${app.dossier-exports.cleanup-delay-ms:60000}")
    public void cleanup() {
        if(!enabled) return;
        String idle="expires_at<? AND (download_until IS NULL OR download_until<CURRENT_TIMESTAMP(6))";
        var rows=jdbc.queryForList("SELECT id FROM dossier_export_jobs j WHERE "+idle+" AND (status IN ('COMPLETED','COMPLETED_WITH_WARNINGS') OR (status IN ('FAILED','CANCELLED','EXPIRED') AND EXISTS (SELECT 1 FROM dossier_export_entries e WHERE e.job_id=j.id))) ORDER BY expires_at LIMIT 20",DossierStorageService.now());
        for(var row:rows) {
            String id=row.get("id").toString();
            if(jdbc.update("UPDATE dossier_export_jobs SET status=CASE WHEN status IN ('FAILED','CANCELLED') THEN status ELSE 'EXPIRED' END WHERE id=? AND status IN ('COMPLETED','COMPLETED_WITH_WARNINGS','FAILED','CANCELLED','EXPIRED') AND "+idle,id,DossierStorageService.now())==1) {
                deleteTree(root.resolve(id));
                jdbc.update("DELETE FROM dossier_export_parts WHERE job_id=?",id);
                jdbc.update("DELETE FROM dossier_export_entries WHERE job_id=?",id);
            }
        }
    }
    /** Quoted for CSV and neutralised so that a spreadsheet never runs a file name as a formula. */
    static String csv(String value) {
        String safe=!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0))>=0 ? "'"+value : value;
        return "\""+safe.replace("\"","\"\"")+"\"";
    }
    private static MessageDigest digest() {try {return MessageDigest.getInstance("SHA-256");} catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private static String hash(byte[] value) {return HexFormat.of().formatHex(digest().digest(value));}
}
