package com.example.app.dossier.service;

import com.example.app.dossier.storage.*;
import com.example.app.intake.service.ReceiptService;
import com.example.app.intake.dto.*;
import com.example.app.intake.entity.*;
import com.example.app.intake.repository.ReceiptAttachmentRepository;
import com.example.app.user.entity.*;
import com.example.app.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.zip.ZipFile;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class DossierStorageIntegrationTest {
    @TempDir static Path temp;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.storage.allowed-roots",()->temp.toString());
        r.add("app.storage.root",()->temp.resolve("receipts").toString());
        r.add("app.storage.results-root",()->temp.resolve("results").toString());
        r.add("app.storage.reserve-bytes",()->0);
        r.add("app.dossier-exports.worker-enabled",()->false);
    }
    @Autowired DossierStorageService storage;
    @Autowired ReceiptService receipts;
    @Autowired ReceiptAttachmentRepository attachments;
    @Autowired DossierCatalog catalog;
    @Autowired DossierExportJobs jobs;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    User admin;
    @BeforeEach void setup() {admin=users.saveAndFlush(new User(UUID.randomUUID()+"@test.local","Admin test","unused",Role.ADMIN));}
    long receipt(String number) {
        var now=Instant.parse("2026-10-31T18:00:00Z");
        return receipts.create(new CreateReceiptRequest("Khach test","0900000000",number,null,"Dia chi test","Nguoi lay",now,
            "Vung test",null,null,null,null,"Nguoi giao",now,"Mau test","1",null,null,SealStatus.INTACT,"Nguyen ven",null,null,null,
            null,"Cadimi",LocalDate.of(2026,11,30),null,AcceptanceDecision.ACCEPTED,null,
            List.of(new SampleRequest("TEST","SR","Mau test","1",null,"Nguyen ven","Cadimi"))),admin).id();
    }
    long register(String label) throws Exception {
        Path dir=Files.createDirectory(temp.resolve(label+UUID.randomUUID()));
        return storage.register(label,dir.toString(),admin.getId()).locations().stream().filter(h->h.location().rootPath()!=null && h.location().rootPath().equals(dir.toString())).findFirst().orElseThrow().location().id();
    }
    ReceiptAttachmentResponse upload(long receipt,String text) {
        return receipts.attach(receipt,new MockMultipartFile("file","anh.pdf","application/pdf",text.getBytes()),ReceiptAttachmentCategory.FORM_3,null,null,admin);
    }
    Path path(ReceiptAttachmentResponse a) {
        var e=attachments.findById(a.id()).orElseThrow();return storage.read(e.getStorageLocationId(),e.getRelativePath(),e.getStorageName());
    }
    @Test void switchingDisksPreservesLegacyAndPreviousFilesAndOneLogicalDossier() throws Exception {
        long id=receipt("CFC26.9901");var legacy=upload(id,"legacy");
        long a=register("A");storage.activate(a,0,admin.getId());var first=upload(id,"diskA");
        long b=register("B");storage.activate(b,1,admin.getId());var second=upload(id,"diskB");
        assertThat(Files.readString(path(legacy))).isEqualTo("legacy");assertThat(Files.readString(path(first))).isEqualTo("diskA");
        assertThat(path(second).toString()).contains("2026/11/CFC26.9901/intake");
        assertThat(attachments.findById(first.id()).orElseThrow().getStorageLocationId()).isEqualTo(a);
        assertThat(attachments.findById(second.id()).orElseThrow().getStorageLocationId()).isEqualTo(b);
        assertThat(catalog.files(id,0,20).totalElements()).isEqualTo(3);
        assertThat(catalog.list(new DossierCatalog.Scope(2026,11,id,null),0,20).totalElements()).isEqualTo(1);
        assertThat(catalog.list(new DossierCatalog.Scope(2026,10,id,null),0,20).totalElements()).isZero();
        assertThatThrownBy(()->catalog.file(id+99999,"RECEIPT",first.id())).isInstanceOf(ResponseStatusException.class);
    }
    @Test void staleConfigAndMissingMountAreRejectedWithoutRedirectingOldFiles() throws Exception {
        long id=receipt("CFC26.9902"),a=register("A"),b=register("B");
        storage.activate(a,0,admin.getId());var old=upload(id,"safe");
        assertThatThrownBy(()->storage.activate(b,0,admin.getId())).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        Path root=Path.of(storage.overview().locations().stream().filter(h->h.location().id()==a).findFirst().orElseThrow().location().rootPath());
        String marker=Files.readString(root.resolve(".cfc-storage-id"));Files.delete(root.resolve(".cfc-storage-id"));
        assertThatThrownBy(()->path(old)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("503");
        assertThatThrownBy(()->upload(id,"not-saved")).isInstanceOf(ResponseStatusException.class);
        assertThat(catalog.files(id,0,20).totalElements()).isEqualTo(1);
        Files.writeString(root.resolve(".cfc-storage-id"),marker);
        assertThat(Files.readString(path(old))).isEqualTo("safe");
    }
    @Test void blocksTraversalSymlinksAndUnapprovedRoots() throws Exception {
        long a=register("A");
        assertThatThrownBy(()->storage.read(a,"../receipts/file",null)).isInstanceOf(ResponseStatusException.class);
        Path outside=Files.createTempDirectory("outside-cfc-test-");
        try {
            assertThatThrownBy(()->storage.register("outside",outside.toString(),admin.getId())).isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
            Path dir=Path.of(storage.overview().locations().stream().filter(h->h.location().id()==a).findFirst().orElseThrow().location().rootPath());
            Files.writeString(outside.resolve("secret"),"not-a-dossier");Files.createSymbolicLink(dir.resolve("link"),outside);
            assertThatThrownBy(()->storage.read(a,"link/secret",null)).isInstanceOf(ResponseStatusException.class);
        } finally {Files.deleteIfExists(outside.resolve("secret"));Files.deleteIfExists(outside);}
    }
    @Test void rolledBackUploadDoesNotLeaveAnUnreferencedFile() throws Exception {
        var file=upload(receipt("CFC26.9903"),"rolled-back");Path path=path(file);assertThat(path).exists();
        TestTransaction.flagForRollback();TestTransaction.end();assertThat(path).doesNotExist();TestTransaction.start();
    }
    @Test void exportCombinesDisksAndSplitsParts() throws Exception {
        long id=receipt("CFC26.9904"),a=register("A");storage.activate(a,0,admin.getId());upload(id,"data-from-A");
        long b=register("B");storage.activate(b,1,admin.getId());upload(id,"data-from-B");
        var scope=new DossierCatalog.Scope(2026,11,id,null);String request=UUID.randomUUID().toString();
        var job=jobs.create(scope,request,admin.getId());String jobId=job.get("id").toString();
        assertThat(jobs.create(scope,request,admin.getId()).get("id")).isEqualTo(jobId);
        Path exports=Files.createDirectory(temp.resolve("exports"));
        var worker=new DossierExportWorker(jobs,catalog,storage,exports.toString(),true,1000000,3,0,100000000);
        worker.runNext();assertThat(jobs.detail(jobId).get("status")).isEqualTo("COMPLETED");
        var parts=jdbc.queryForList("SELECT * FROM dossier_export_parts WHERE job_id=?",jobId);assertThat(parts.size()).isGreaterThan(1);
        List<String> binary=new ArrayList<>();List<String> entries=new ArrayList<>();
        for(var part:parts) try(var zip=new ZipFile(exports.resolve(part.get("relative_path").toString()).toFile())) {
            var it=zip.entries();while(it.hasMoreElements()) {var e=it.nextElement();entries.add(e.getName());
                if(e.getName().contains("01-tiep-nhan")) binary.add(new String(zip.getInputStream(e).readAllBytes()));}
            assertThat(zip.getEntry("danh-muc-goi-"+part.get("part_number")+".csv")).isNotNull();
        }
        assertThat(binary).containsExactlyInAnyOrder("data-from-A","data-from-B");
        assertThat(entries).contains("2026/11/CFC26.9904/ho-so.json","2026/11/CFC26.9904/danh-muc-file.csv");
        assertThat(entries).doesNotHaveDuplicates();
        assertThat(jobs.list(0)).anyMatch(j->jobId.equals(j.get("id")) && "CFC26.9904".equals(j.get("receipt_number")));
        assertThat(entries).noneMatch(s->s.contains("users/") || s.contains("auth_sessions"));
        jdbc.update("UPDATE dossier_export_jobs SET expires_at=DATE_SUB(CURRENT_TIMESTAMP(6),INTERVAL 1 DAY) WHERE id=?",jobId);
        worker.cleanup();assertThat(jobs.detail(jobId).get("status")).isEqualTo("EXPIRED");
        assertThat(Files.readString(path(attachmentsResponse(id)))).isEqualTo("data-from-A");
    }
    private ReceiptAttachmentResponse attachmentsResponse(long id) {return receipts.findById(id).attachments().getFirst();}
    @Test void corruptSourceFailsExportAndQueuedCancelDoesNotRun() throws Exception {
        long id=receipt("CFC26.9905");var attachment=upload(id,"original");Files.writeString(path(attachment),"corrupt!");
        var scope=new DossierCatalog.Scope(null,null,id,null);String jobId=jobs.create(scope,UUID.randomUUID().toString(),admin.getId()).get("id").toString();
        Path exports=Files.createDirectory(temp.resolve("broken-exports"));
        new DossierExportWorker(jobs,catalog,storage,exports.toString(),true,1000000,100,0,100000000).runNext();
        assertThat(jobs.detail(jobId)).containsEntry("status","FAILED").containsEntry("error_code","SOURCE_MODIFIED");
        assertThat(files(exports)).isEmpty();
        var queued=jobs.create(scope,UUID.randomUUID().toString(),admin.getId());jobs.cancel(queued.get("id").toString(),admin.getId());
        assertThat(jobs.detail(queued.get("id").toString()).get("status")).isEqualTo("CANCELLED");
    }
    @Test void onlyAdminCanAccessStorageArchiveAndExportEndpoints() throws Exception {
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for(String uri:List.of("/api/v1/admin/storage-locations","/api/v1/admin/dossiers","/api/v1/admin/dossier-exports","/api/v1/admin/dossiers/1/files/RECEIPT/1")) {
            mvc.perform(get(uri)).andExpect(status().isUnauthorized());
            for(String role:List.of("RECEPTIONIST","TECHNICIAN")) mvc.perform(get(uri).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_"+role)))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/admin/storage-locations").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))).andExpect(status().isOk());
        mvc.perform(put("/api/v1/admin/storage-write-target").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN"))).with(csrf()).contentType("application/json").content("{\"locationId\":1,\"version\":0}")).andExpect(status().isForbidden());
    }

    private static List<Path> files(Path dir) throws Exception {
        try(var walk=Files.walk(dir)) {return walk.filter(Files::isRegularFile).toList();}
    }
    private DossierExportWorker worker(Path exports) {return new DossierExportWorker(jobs,catalog,storage,exports.toString(),true,100000000,5000,0,100000000);}
    private Map<String,String> unzip(Path exports,String jobId) throws Exception {
        Map<String,String> all=new LinkedHashMap<>();
        for(var part:jdbc.queryForList("SELECT relative_path FROM dossier_export_parts WHERE job_id=?",jobId))
            try(var zip=new ZipFile(exports.resolve(part.get("relative_path").toString()).toFile())) {
                var it=zip.entries();while(it.hasMoreElements()) {var e=it.nextElement();all.put(e.getName(),new String(zip.getInputStream(e).readAllBytes()));}
            }
        return all;
    }
    private void rename(ReceiptAttachmentResponse a,String name) {
        attachments.flush();
        jdbc.update("UPDATE receipt_attachments SET storage_name=?,relative_path=NULL WHERE id=?",name,a.id());
    }
    @Test void legacyNamesWithColonStayReadable() throws Exception {
        long id=receipt("CFC26.9906");var file=upload(id,"phone-photo");Path old=path(file);
        String name=UUID.randomUUID()+"-Anh 10:30.jpg";Files.move(old,old.resolveSibling(name));rename(file,name);
        assertThat(Files.readString(catalog.open(catalog.file(id,"RECEIPT",file.id())))).isEqualTo("phone-photo");
    }
    @Test void fullDiskRejectsUploadWith507AndLeavesNothingBehind() throws Exception {
        long id=receipt("CFC26.9907"),a=register("Full");storage.activate(a,0,admin.getId());
        Path root=Path.of(location(a).rootPath());
        var full=new DossierStorageService(jdbc,temp.resolve("receipts").toString(),temp.resolve("results").toString(),temp.toString(),
            Long.MAX_VALUE/4,0,0,temp.resolve("x").toString(),temp.resolve("y").toString());
        assertThatThrownBy(()->full.write(1,id,"intake","a.bin",new java.io.ByteArrayInputStream(new byte[]{1}),1))
            .isInstanceOf(ResponseStatusException.class).hasMessageContaining("507");
        assertThat(files(root)).extracting(p->p.getFileName().toString()).containsExactly(".cfc-storage-id");
        assertThat(full.overview().locations()).filteredOn(h->h.location().id()==a).allMatch(DossierStorageService.Health::warning);
    }
    @Test void truncatedUploadIsNotKept() throws Exception {
        long id=receipt("CFC26.9908"),a=register("Cut");storage.activate(a,0,admin.getId());
        assertThatThrownBy(()->storage.write(1,id,"intake","a.bin",new java.io.ByteArrayInputStream(new byte[]{1,2}),5)).isInstanceOf(ResponseStatusException.class);
        assertThat(files(Path.of(location(a).rootPath()))).hasSize(1);
    }
    private DossierStorageService.Location location(long id) {
        return storage.overview().locations().stream().map(DossierStorageService.Health::location).filter(l->l.id()==id).findFirst().orElseThrow();
    }
    @Test void renamedDeviceKeepsReadingButBlocksUploadUntilValidated() throws Exception {
        long id=receipt("CFC26.9909"),a=register("Dev");storage.activate(a,0,admin.getId());var old=upload(id,"still-here");
        jdbc.update("UPDATE storage_locations SET volume_identity='/dev/old|ext4' WHERE id=?",a);
        assertThat(Files.readString(path(old))).isEqualTo("still-here");
        assertThat(storage.overview().locations()).filteredOn(h->h.location().id()==a).extracting(DossierStorageService.Health::status).containsExactly("VOLUME_CHANGED");
        assertThatThrownBy(()->storage.write(1,id,"intake","b.bin",new java.io.ByteArrayInputStream(new byte[]{1}),1)).hasMessageContaining("503");
        storage.validate(a,admin.getId());
        assertThat(upload(id,"after")).isNotNull();
    }
    @Test void resultFilesFollowTheSelectedDiskAndDossierFolder() throws Exception {
        long id=receipt("CFC26.9910"),a=register("Res");storage.activate(a,0,admin.getId());
        var written=storage.write(2,id,"results/7/9","r.pdf",new java.io.ByteArrayInputStream("pdf".getBytes()),3);
        assertThat(written.locationId()).isEqualTo(a);
        assertThat(written.relativePath()).isEqualTo("2026/11/CFC26.9910/results/7/9/r.pdf");
        assertThat(Files.readString(storage.read(a,written.relativePath(),"ignored"))).isEqualTo("pdf");
    }
    @Test void invalidPathsAndReservedAreasAreRejectedAsBadRequest() throws Exception {
        // 401 would log the Admin out in the browser.
        assertThatThrownBy(()->storage.register("nul","/tmp/\0x",admin.getId())).isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        Path exports=Files.createDirectories(temp.resolve("reserved-exports").resolve("inside"));
        var guarded=new DossierStorageService(jdbc,temp.resolve("receipts").toString(),temp.resolve("results").toString(),temp.toString(),0,0,0,
            temp.resolve("reserved-exports").toString(),temp.resolve("y").toString());
        assertThatThrownBy(()->guarded.register("exports",exports.toString(),admin.getId())).hasMessageContaining("400");
    }
    @Test void missingFileFailsExportUnlessAdminAllowsAWarningPackage() throws Exception {
        long id=receipt("CFC26.9911");var kept=upload(id,"kept");var lost=upload(id,"lost");var changed=upload(id,"original");
        Files.delete(path(lost));Files.writeString(path(changed),"tampered-content");assertThat(kept).isNotNull();
        var scope=new DossierCatalog.Scope(null,null,id,null);Path exports=Files.createDirectory(temp.resolve("warn-exports"));
        String strict=jobs.create(scope,UUID.randomUUID().toString(),admin.getId()).get("id").toString();worker(exports).runNext();
        assertThat(jobs.detail(strict)).containsEntry("status","FAILED").containsEntry("error_code","SOURCE_UNAVAILABLE");
        String loose=jobs.create(scope,true,UUID.randomUUID().toString(),admin.getId()).get("id").toString();worker(exports).runNext();
        assertThat(jobs.detail(loose)).containsEntry("status","COMPLETED_WITH_WARNINGS").containsEntry("warning_count",2L);
        var zip=unzip(exports,loose);
        assertThat(zip.get("danh-muc-goi-1.csv")).contains("THIEU_FILE").contains("KHAC_CHECKSUM");
        assertThat(zip.keySet()).noneMatch(k->k.contains("receipt-"+lost.id()+"-"));
    }
    @Test void exportHidesLookupCodeAndServerPathsAndNeutralisesSpreadsheetFormulas() throws Exception {
        long id=receipt("CFC26.9912");upload(id,"x");
        String jobId=jobs.create(new DossierCatalog.Scope(null,null,id,null),UUID.randomUUID().toString(),admin.getId()).get("id").toString();
        Path exports=Files.createDirectory(temp.resolve("hidden-exports"));worker(exports).runNext();
        var zip=unzip(exports,jobId);
        assertThat(zip.keySet()).anyMatch(k->k.contains("05-du-lieu/receipt_attachments/"));
        assertThat(zip.values()).noneMatch(v->v.contains("lookup_code") || v.contains("storage_name") || v.contains(temp.toString()));
        assertThat(DossierExportWorker.csv("=HYPERLINK(1)")).isEqualTo("\"'=HYPERLINK(1)\"");
    }
    @Test void fileUploadedAfterTheRequestIsNotInTheSnapshot() throws Exception {
        long id=receipt("CFC26.9913");upload(id,"before");
        String jobId=jobs.create(new DossierCatalog.Scope(null,null,id,null),UUID.randomUUID().toString(),admin.getId()).get("id").toString();
        Thread.sleep(5);upload(id,"after-cutoff");
        Path exports=Files.createDirectory(temp.resolve("cutoff-exports"));worker(exports).runNext();
        assertThat(unzip(exports,jobId).values()).contains("before").doesNotContain("after-cutoff");
    }
    @Test void plannedRestartRequeuesAndCrashedJobStopsAfterThreeAttempts() throws Exception {
        long id=receipt("CFC26.9914");var scope=new DossierCatalog.Scope(null,null,id,null);
        String jobId=jobs.create(scope,UUID.randomUUID().toString(),admin.getId()).get("id").toString();
        Path exports=Files.createDirectory(temp.resolve("restart-exports"));
        var stopped=worker(exports);stopped.stop();stopped.runNext();
        assertThat(jobs.detail(jobId)).containsEntry("status","QUEUED");assertThat(files(exports)).isEmpty();
        for(int i=0;i<3;i++) {
            assertThat(jobs.claim()).isNotNull();
            assertThat(jobs.claim()).as("second worker while the lease is alive").isNull();
            jdbc.update("UPDATE dossier_export_jobs SET lease_until=DATE_SUB(CURRENT_TIMESTAMP(6),INTERVAL 1 MINUTE) WHERE id=?",jobId);
            jdbc.update("UPDATE dossier_export_worker SET lease_until=DATE_SUB(CURRENT_TIMESTAMP(6),INTERVAL 1 MINUTE)");
        }
        assertThat(jobs.claim()).isNull();
        assertThat(jobs.detail(jobId)).containsEntry("status","FAILED").containsEntry("error_code","TOO_MANY_ATTEMPTS");
    }
    @Test void cancelDuringPackingEndsCancelledWithoutLeftovers() throws Exception {
        long id=receipt("CFC26.9915");upload(id,"x");
        String jobId=jobs.create(new DossierCatalog.Scope(null,null,id,null),UUID.randomUUID().toString(),admin.getId()).get("id").toString();
        Path exports=Files.createDirectory(temp.resolve("cancel-exports"));
        var cancelling=new DossierCatalog(jdbc,storage) {
            @Override public List<DossierCatalog.FileRow> fileBatch(long receiptId,String source,long afterId,Instant cutoff) {
                jdbc.update("UPDATE dossier_export_jobs SET status='CANCEL_REQUESTED' WHERE id=?",jobId);
                return super.fileBatch(receiptId,source,afterId,cutoff);
            }
        };
        new DossierExportWorker(jobs,cancelling,storage,exports.toString(),true,100000000,5000,0,100000000).runNext();
        assertThat(jobs.detail(jobId)).containsEntry("status","CANCELLED");assertThat(files(exports)).isEmpty();
    }
    @Test void cleanupReachesNewPackagesBehindManyExpiredOnes() {
        for(int i=0;i<25;i++) jdbc.update("INSERT INTO dossier_export_jobs(id,request_id,actor_user_id,scope_json,status,cutoff_at,expires_at) VALUES (?,?,?,'{}','EXPIRED',CURRENT_TIMESTAMP(6),DATE_SUB(CURRENT_TIMESTAMP(6),INTERVAL 30 DAY))","old-"+i,"old-"+i,admin.getId());
        jdbc.update("INSERT INTO dossier_export_jobs(id,request_id,actor_user_id,scope_json,status,cutoff_at,expires_at) VALUES ('fresh','fresh',?,'{}','COMPLETED',CURRENT_TIMESTAMP(6),DATE_SUB(CURRENT_TIMESTAMP(6),INTERVAL 1 DAY))",admin.getId());
        worker(temp.resolve("none")).cleanup();
        assertThat(jobs.detail("fresh")).containsEntry("status","EXPIRED");
    }
    @Test void legacyDuplicateNumbersGetSeparateFoldersAndExportTogether() throws Exception {
        long first=receipt("CFC26.9919"),second=receipt("CFC26.9920");upload(first,"one");upload(second,"two");
        // V16 keeps old duplicates: only the first dossier owns the number.
        jdbc.update("UPDATE receipts SET request_number='CFC26.9919',request_number_unique=NULL WHERE id=?",second);
        assertThat(catalog.folder(catalog.receipt(first))).isEqualTo("2026/11/CFC26.9919");
        assertThat(catalog.folder(catalog.receipt(second))).isEqualTo("2026/11/CFC26.9919-r"+second);
        assertThat(storage.folder(second)).isEqualTo("2026/11/CFC26.9919-r"+second);
        String jobId=jobs.create(new DossierCatalog.Scope(2026,11,null,"CFC26.9919"),UUID.randomUUID().toString(),admin.getId()).get("id").toString();
        Path exports=Files.createDirectory(temp.resolve("duplicate-exports"));worker(exports).runNext();
        assertThat(jobs.detail(jobId)).containsEntry("status","COMPLETED");
        assertThat(unzip(exports,jobId)).containsKeys("2026/11/CFC26.9919/ho-so.json","2026/11/CFC26.9919-r"+second+"/ho-so.json");
        // A folder that was already fixed on disk before this rule must still not collide inside a package.
        jdbc.update("UPDATE receipts SET archive_path='2026/11/CFC26.9919' WHERE id=?",second);
        assertThat(catalog.folder(catalog.receipt(second))).isEqualTo("2026/11/CFC26.9919-r"+second);
    }
    @Test void listShowsCustomerAndSupportsSorting() {
        long a=receipt("CFC26.9921"),b=receipt("CFC26.9922");
        var scope=new DossierCatalog.Scope(2026,11,null,"CFC26.992");
        assertThat(catalog.list(scope,"requestNumber,asc",0,20).content()).extracting(r->r.get("id")).containsExactly(a,b);
        assertThat(catalog.list(scope,"requestNumber,desc",0,20).content()).extracting(r->r.get("id")).containsExactly(b,a);
        assertThat(catalog.list(scope,0,20).content().getFirst()).containsEntry("customerName","Khach test").containsEntry("sampleName","Mau test");
        assertThatThrownBy(()->catalog.list(scope,"id;DROP,asc",0,20)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
    }
    @Test void perAdminQueueLimit() {
        long id=receipt("CFC26.9916");
        for(int i=0;i<5;i++) jobs.create(new DossierCatalog.Scope(null,null,id,null),"r"+i,admin.getId());
        assertThatThrownBy(()->jobs.create(new DossierCatalog.Scope(null,null,id,null),"r5",admin.getId())).hasMessageContaining("429");
        assertThatThrownBy(()->jobs.create(new DossierCatalog.Scope(null,null,id,null),true,"r0",admin.getId())).hasMessageContaining("409");
    }
    @Test void summaryAndPeriodsUseVietnamReceivingMonthForTheWholeFilter() {
        long id=receipt("CFC26.9917");upload(id,"12345");
        assertThat(catalog.summary(new DossierCatalog.Scope(2026,11,id,null))).isEqualTo(new DossierCatalog.Summary(1,1,5));
        assertThat(catalog.summary(new DossierCatalog.Scope(2026,10,id,null))).isEqualTo(new DossierCatalog.Summary(0,0,0));
        jobs.create(new DossierCatalog.Scope(2026,11,null,null),UUID.randomUUID().toString(),admin.getId());
        assertThat(jobs.list(0)).anyMatch(j->j.get("receipt_number")==null);
        assertThat(catalog.periods()).anyMatch(p->p.year()==2026 && p.month()==11 && p.dossiers()>=1);
        var row=catalog.list(new DossierCatalog.Scope(2026,null,id,null),0,20).content().getFirst();
        assertThat(row).containsEntry("fileCount",1L).containsEntry("totalBytes",5L);
        assertThat(catalog.file(id,"RECEIPT",catalog.files(id,0,10).content().getFirst().id()).uploadedBy()).isEqualTo("Admin test");
    }
    @Test void partDownloadSupportsRangesAndDetailHidesServerPaths() throws Exception {
        long id=receipt("CFC26.9918");var file=upload(id,"x");
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var asAdmin=jwt().jwt(j->j.subject(admin.getEmail())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        String detail=mvc.perform(get("/api/v1/admin/dossiers/"+id).with(asAdmin)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(detail).contains("\"folder\":\"2026/11/CFC26.9918\"").contains("checksum").doesNotContain("storageName").doesNotContain("relativePath").doesNotContain("locationId");
        mvc.perform(get("/api/v1/admin/dossiers/"+id+"/files/RECEIPT/"+file.id()).with(asAdmin)).andExpect(status().isOk());
        assertThat(catalog.audit(0)).anyMatch(a->"DOSSIER_DOWNLOAD".equals(a.get("action")) && ("RECEIPT:"+file.id()).equals(a.get("object_id")));
        for(String uri:List.of("/api/v1/admin/dossiers/summary?year=2026","/api/v1/admin/dossiers/periods","/api/v1/admin/dossiers?year=2026"))
            mvc.perform(get(uri).with(asAdmin)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/dossiers?month=5").with(asAdmin)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/dossier-exports/none/parts/1").with(asAdmin)).andExpect(status().isNotFound());
    }
    /** Real concurrency needs committed rows, so these two clean up after themselves. */
    @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void twoWorkersNeverClaimTheSameJobAndTwoAdminsNeverBothSwitch() throws Exception {
        String jobId=jobs.create(new DossierCatalog.Scope(1999+2,1,null,null),UUID.randomUUID().toString(),admin.getId()).get("id").toString();
        Long a=null,b=null;
        try {
            var claims=race(()->jobs.claim(),()->jobs.claim());
            assertThat(claims.stream().filter(c->c instanceof DossierExportJobs.Claim).count()).isEqualTo(1);
            a=register("RaceA");b=register("RaceB");long first=a,second=b;
            long version=storage.settings().version();
            var results=race(()->storage.activate(first,version,admin.getId()),()->storage.activate(second,version,admin.getId()));
            assertThat(results.stream().filter(x->x instanceof DossierStorageService.Overview).count()).isEqualTo(1);
            assertThat(storage.settings().version()).isEqualTo(version+1);
        } finally {
            jdbc.update("UPDATE storage_write_settings SET location_id=NULL,version=0");
            jdbc.update("UPDATE dossier_export_worker SET lease_token=NULL,lease_until=NULL");
            jdbc.update("DELETE FROM dossier_export_jobs WHERE id=?",jobId);
            if(a!=null) jdbc.update("DELETE FROM storage_locations WHERE id=?",a);
            if(b!=null) jdbc.update("DELETE FROM storage_locations WHERE id=?",b);
            jdbc.update("DELETE FROM dossier_audit WHERE actor_user_id=?",admin.getId());
            jdbc.update("DELETE FROM users WHERE id=?",admin.getId());
        }
    }
    private static List<Object> race(java.util.concurrent.Callable<Object> one,java.util.concurrent.Callable<Object> two) throws Exception {
        var start=new java.util.concurrent.CountDownLatch(1);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var futures=java.util.stream.Stream.of(one,two).map(c->pool.submit(()->{start.await();try {return c.call();} catch(Exception e) {return e;}})).toList();
            start.countDown();List<Object> out=new ArrayList<>();for(var f:futures) out.add(f.get()==null?"none":f.get());return out;
        }
    }
}
