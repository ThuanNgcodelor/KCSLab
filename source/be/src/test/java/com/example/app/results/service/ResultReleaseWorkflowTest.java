package com.example.app.results.service;

import com.example.app.intake.entity.AcceptanceDecision;
import com.example.app.intake.entity.Customer;
import com.example.app.intake.entity.Receipt;
import com.example.app.intake.entity.Sample;
import com.example.app.intake.repository.CustomerRepository;
import com.example.app.intake.repository.ReceiptRepository;
import com.example.app.results.dto.ResultReleaseDetailResponse;
import com.example.app.testing.dto.EquipmentRequest;
import com.example.app.testing.dto.MeasurementRequest;
import com.example.app.testing.dto.ReviewTestingRecordRequest;
import com.example.app.testing.dto.SaveTestingRecordRequest;
import com.example.app.testing.entity.MeasurementType;
import com.example.app.testing.entity.TestRecordStatus;
import com.example.app.testing.service.TestingRecordService;
import com.example.app.transfer.dto.CreateTransferItemRequest;
import com.example.app.transfer.dto.CreateTransferRequest;
import com.example.app.transfer.dto.TransferDecisionRequest;
import com.example.app.transfer.entity.TransferStatus;
import com.example.app.transfer.service.TransferService;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "app.storage.results-root=target/test-storage/results")
class ResultReleaseWorkflowTest {
    @Autowired private ResultReleaseService workflow;
    @Autowired private ResultReleaseDraftService drafts;
    @Autowired private TestingRecordService records;
    @Autowired private TransferService transfers;
    @Autowired private UserRepository users;
    @Autowired private CustomerRepository customers;
    @Autowired private ReceiptRepository receipts;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.example.app.user.service.UserService userService;
    @Autowired private EntityManager entityManager;
    @Autowired private com.example.app.testing.service.TestingRecordService testingRecords;
    @Autowired private com.example.app.notification.service.StaffNotificationService notifications;

    private User technician;
    private User otherTechnician;
    private User admin;
    private Long releaseId;
    private Long sampleId;

    @BeforeEach
    void setUp() {
        User receptionist = users.save(new User("wf-rec@test.local", "Tiếp nhận", "unused", Role.RECEPTIONIST));
        technician = users.save(new User("wf-tech@test.local", "KTV lập hồ sơ", "unused", Role.TECHNICIAN));
        otherTechnician = users.save(new User("wf-tech2@test.local", "KTV khác", "unused", Role.TECHNICIAN));
        admin = users.save(new User("wf-admin@test.local", "Admin duyệt", "unused", Role.ADMIN));
        Customer customer = customers.save(new Customer("Khách kết quả", "0900000777", null, null));
        Receipt receipt = new Receipt("TN-WF-001", customer, receptionist, "Người giao", Instant.now(),
                "Sầu riêng", "1", "Túi", null, true, "Nguyên vẹn", null, null, null, "Thường", "Cadimi",
                LocalDate.now().plusDays(5), null, AcceptanceDecision.ACCEPTED, null);
        receipt.addSample(new Sample("CT777-SR-01012030-001", "CT777", "Sầu riêng", LocalDate.now(), 1,
                "Mẫu", "1", "Túi", "Tốt", "Cadimi").markLegacyVerified());
        receipt = receipts.save(receipt);
        sampleId = receipt.getSamples().getFirst().getId();
        var transfer = transfers.create(new CreateTransferRequest(receipt.getId(), "KCS", LocalDate.now(),
                LocalDate.now().plusDays(5), false, null, null,
                List.of(new CreateTransferItemRequest(sampleId, null, null, null, null, "PP")))
                , receptionist);
        transfers.submit(transfer.id());
        var accepted = transfers.decide(transfer.id(), new TransferDecisionRequest(
                TransferStatus.ACCEPTED, null, null, false, null), technician);
        Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        var record = records.create(new SaveTestingRecordRequest(accepted.items().getFirst().id(), start,
                start.plus(1, ChronoUnit.HOURS), null, null, new BigDecimal("99.0"), null, null,
                List.<EquipmentRequest>of(), List.of(),
                List.of(new MeasurementRequest(MeasurementType.RAW, "1", "1 g", "10 mL", "1", "0.1", "1", "1", "1%"))),
                technician);
        records.submit(record.id(), technician);
        records.review(record.id(), new ReviewTestingRecordRequest(TestRecordStatus.REVIEWED, "Đạt"), admin);
        releaseId = drafts.create(record.id(), technician).id();
    }

    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile("files", name, ResultFilePolicyTest.PDF, ResultFilePolicyTest.pdf("1 0 obj <<>> endobj"));
    }

    private MockMultipartFile docx(String name) throws Exception {
        return new MockMultipartFile("files", name, ResultFilePolicyTest.DOCX, ResultFilePolicyTest.docx());
    }

    private ResultReleaseDetailResponse fillReport() {
        return workflow.updateReport(releaseId, List.of(
                new ResultReleaseService.ReportRowInput("Auramine O (Phần vỏ)", "µg/kg", "CFC-01:2026 (1)(2)(3)", "KPH", "LOQ = 1,5 µg/kg"),
                new ResultReleaseService.ReportRowInput("Cadimium (Cd)", "µg/kg", "CFC-02:2026 (1)(2)(3)", "< LOQ", "LOQ = 30 µg/kg")),
                "Phan Trường Tiền", "Nguyễn Công Huân", technician);
    }

    private static String pdfText(byte[] pdf) throws Exception {
        var reader = new com.lowagie.text.pdf.PdfReader(pdf);
        var extractor = new com.lowagie.text.pdf.parser.PdfTextExtractor(reader);
        StringBuilder text = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) text.append(extractor.getTextFromPage(page)).append('\n');
        return text.toString();
    }

    private void expect(int status, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).as(e.getReason()).isEqualTo(status));
    }

    @Test
    void fullLifecycleUploadSubmitPublishRevokeAndRevise() throws Exception {
        var withFiles = workflow.addFiles(releaseId, List.of(pdf("kq.pdf"), docx("kq.docx")), technician);
        assertThat(withFiles.files()).hasSize(2);
        assertThat(withFiles.files()).allSatisfy(f -> {
            assertThat(f.checksum()).hasSize(64);
            assertThat(f.publicDownload()).isFalse();
        });

        fillReport();
        assertThat(workflow.submit(releaseId, technician).status()).isEqualTo("SUBMITTED");
        var published = workflow.publish(releaseId, null, admin);

        assertThat(published.status()).isEqualTo("PUBLISHED");
        assertThat(published.lookupCode()).hasSize(16).matches("[A-Z2-9]+");
        assertThat(published.resultNumber()).matches("SR\\d{6}-\\d{3}");
        // The system-generated KQKN PDF is the public document; uploaded attachments stay internal by default.
        assertThat(published.files()).hasSize(3);
        assertThat(published.files()).filteredOn(ResultReleaseDetailResponse.FileResponse::publicDownload)
                .extracting(ResultReleaseDetailResponse.FileResponse::kind).containsExactly("GENERATED_REPORT");
        var generated = published.files().stream().filter(f -> f.kind().equals("GENERATED_REPORT")).findFirst().orElseThrow();
        assertThat(generated.originalName()).isEqualTo("KQKN-" + published.resultNumber() + ".pdf");
        assertThat(generated.contentType()).isEqualTo("application/pdf");
        expect(409, () -> workflow.addFiles(releaseId, List.of(pdf("late.pdf")), technician));

        var revoked = workflow.revoke(releaseId, "Phát hiện sai số liệu", admin);
        assertThat(revoked.status()).isEqualTo("REVOKED");
        var next = workflow.revise(releaseId, technician);
        assertThat(next.version()).isEqualTo(2);
        assertThat(next.status()).isEqualTo("DRAFT");
        expect(409, () -> workflow.revise(releaseId, technician));

        assertThat(workflow.history(releaseId)).extracting(ResultReleaseDetailResponse.EventResponse::eventType)
                .containsExactly("CREATED", "FILE_ADDED", "FILE_ADDED", "REPORT_UPDATED", "SUBMITTED", "PUBLISHED", "REVOKED");
    }

    @Test
    void submitNeedsFilesAndOnlyTheCreatingTechnicianMayUploadOrSubmit() throws Exception {
        expect(409, () -> workflow.submit(releaseId, technician)); // no result rows yet
        expect(403, () -> workflow.addFiles(releaseId, List.of(pdf("a.pdf")), otherTechnician));
        expect(403, () -> workflow.addFiles(releaseId, List.of(pdf("a.pdf")), admin));
        workflow.addFiles(releaseId, List.of(pdf("a.pdf")), technician);
        expect(409, () -> workflow.submit(releaseId, technician)); // attachments alone are not a report
        fillReport();
        expect(403, () -> workflow.submit(releaseId, otherTechnician));
    }

    @Test
    void invalidFileInABatchStoresNothingAndLimitsAreEnforced() throws Exception {
        var bad = new MockMultipartFile("files", "x.pdf", ResultFilePolicyTest.PDF, "MZ".getBytes());
        expect(415, () -> workflow.addFiles(releaseId, List.of(pdf("ok.pdf"), bad), technician));
        assertThat(workflow.detail(releaseId).files()).isEmpty();

        var many = new java.util.ArrayList<MockMultipartFile>();
        for (int i = 0; i < 11; i++) many.add(pdf("f" + i + ".pdf"));
        expect(409, () -> workflow.addFiles(releaseId, List.copyOf(many), technician));
    }

    @Test
    void returnedDossierBecomesEditableAgainAndNeedsAReason() throws Exception {
        workflow.addFiles(releaseId, List.of(pdf("a.pdf")), technician);
        fillReport();
        workflow.submit(releaseId, technician);
        expect(400, () -> workflow.giveBack(releaseId, " ", admin));
        expect(403, () -> workflow.giveBack(releaseId, "Thiếu", technician));
        assertThat(workflow.giveBack(releaseId, "Thiếu chữ ký", admin).status()).isEqualTo("RETURNED");

        var edited = workflow.addFiles(releaseId, List.of(pdf("b.pdf")), technician);
        assertThat(edited.files()).hasSize(2);
        assertThat(workflow.submit(releaseId, technician).status()).isEqualTo("SUBMITTED");
    }

    @Test
    void publishRequiresSubmittedAdminAndVerifiedSampleCoding() throws Exception {
        fillReport();
        expect(409, () -> workflow.publish(releaseId, null, admin));
        workflow.submit(releaseId, technician);
        expect(403, () -> workflow.publish(releaseId, null, technician));

        entityManager.flush();
        jdbc.update("update samples set coding_status = 'PENDING_VERIFICATION' where id = ?", sampleId);
        entityManager.clear();
        expect(409, () -> workflow.publish(releaseId, null, admin));

        jdbc.update("update samples set coding_status = 'VERIFIED' where id = ?", sampleId);
        entityManager.clear();
        assertThat(workflow.publish(releaseId, null, admin).status()).isEqualTo("PUBLISHED");
    }

    @Test
    void adminCanExplicitlyPublishExtraAttachmentsAndTheReportIsAlwaysPublic() throws Exception {
        var detail = workflow.addFiles(releaseId, List.of(pdf("a.pdf"), docx("b.docx")), technician);
        Long docxId = detail.files().get(1).id();
        fillReport();
        workflow.submit(releaseId, technician);

        expect(400, () -> workflow.publish(releaseId, Set.of(999999L), admin));
        var published = workflow.publish(releaseId, Set.of(docxId), admin);
        assertThat(published.files()).filteredOn(ResultReleaseDetailResponse.FileResponse::publicDownload)
                .extracting(ResultReleaseDetailResponse.FileResponse::originalName)
                .containsExactlyInAnyOrder("b.docx", "KQKN-" + published.resultNumber() + ".pdf");
    }

    @Test
    void reportContentIsValidatedAndOnlyTheCreatorEditsItWhileEditable() {
        expect(400, () -> workflow.updateReport(releaseId, List.of(), "A", "B", technician));
        expect(400, () -> workflow.updateReport(releaseId, List.of(new ResultReleaseService.ReportRowInput(" ", "u", "m", "KPH", null)), "A", "B", technician));
        expect(400, () -> workflow.updateReport(releaseId, List.of(new ResultReleaseService.ReportRowInput("Cd", "u", "m", " ", null)), "A", "B", technician));
        expect(403, () -> workflow.updateReport(releaseId, List.of(new ResultReleaseService.ReportRowInput("Cd", "u", "m", "KPH", null)), "A", "B", otherTechnician));
        expect(403, () -> workflow.updateReport(releaseId, List.of(new ResultReleaseService.ReportRowInput("Cd", "u", "m", "KPH", null)), "A", "B", admin));
        var saved = fillReport();
        assertThat(saved.items()).extracting(ResultReleaseDetailResponse.ItemResponse::parameter)
                .containsExactly("Auramine O (Phần vỏ)", "Cadimium (Cd)");
        assertThat(saved.qualityHeadName()).isEqualTo("Phan Trường Tiền");
        workflow.submit(releaseId, technician);
        expect(409, () -> fillReport()); // locked once submitted
    }

    @Test
    void generatedPdfFollowsTheTemplateAndIsStoredWithChecksum() throws Exception {
        fillReport();
        byte[] preview = workflow.previewPdf(releaseId);
        String previewText = pdfText(preview);
        assertThat(new String(preview, 0, 5)).isEqualTo("%PDF-");
        assertThat(previewText).contains("PHIẾU KẾT QUẢ THỬ NGHIỆM", "TEST REPORT", "THÔNG TIN MẪU", "KẾT QUẢ THỬ NGHIỆM",
                "Auramine O (Phần vỏ)", "Cadimium (Cd)", "KPH", "LOQ = 30 µg/kg", "Phan Trường Tiền", "Nguyễn Công Huân",
                "Khách kết quả", "CT777-SR-01012030-001", "BM.QT.14.02", "GHI CHÚ/ NOTES");

        workflow.submit(releaseId, technician);
        var published = workflow.publish(releaseId, null, admin);
        var generated = published.files().stream().filter(f -> f.kind().equals("GENERATED_REPORT")).findFirst().orElseThrow();
        assertThat(generated.checksum()).hasSize(64);
        try (var in = workflow.loadFile(releaseId, generated.id()).resource().getInputStream()) {
            byte[] stored = in.readAllBytes();
            String text = pdfText(stored);
            assertThat(text).contains(published.resultNumber(), published.lookupCode().substring(0, 4), "Auramine O (Phần vỏ)");
            assertThat(text).doesNotContain("BẢN XEM TRƯỚC");
            assertThat(java.security.MessageDigest.getInstance("SHA-256").digest(stored))
                    .isEqualTo(java.util.HexFormat.of().parseHex(generated.checksum()));
        }
    }

    @Test
    void resultNumbersAreSequentialPerPrefixAndNeverRepeat(@org.springframework.beans.factory.annotation.Autowired ResultNumberSequence sequence) {
        assertThat(sequence.next("ZZ012099")).isEqualTo("ZZ012099-001");
        assertThat(sequence.next("ZZ012099")).isEqualTo("ZZ012099-002");
        assertThat(sequence.next("ZZ022099")).isEqualTo("ZZ022099-001");
    }

    @Test
    void removedFilesAreNotServedAndOnlyRevokedDossierCanBeRevised() throws Exception {
        var detail = workflow.addFiles(releaseId, List.of(pdf("a.pdf")), technician);
        Long fileId = detail.files().getFirst().id();
        assertThat(workflow.loadFile(releaseId, fileId).originalName()).isEqualTo("a.pdf");
        workflow.removeFile(releaseId, fileId, technician);
        expect(404, () -> workflow.loadFile(releaseId, fileId));
        expect(409, () -> workflow.revise(releaseId, technician));
        expect(403, () -> workflow.revoke(releaseId, "x", technician));
    }

    // ---- list endpoints: sorting, searching and date filtering are done by the database ----

    @Test
    void transferListSortsFiltersAndHidesCustomerDataFromTechnicians() {
        var byCode = transfers.search(admin, null, null, null, null, 0, 10, "transferCode,asc");
        assertThat(byCode.sort()).isEqualTo("transferCode,asc");
        assertThat(byCode.content()).isNotEmpty();
        assertThat(transfers.search(admin, "khách kết quả", null, null, null, 0, 10, "createdAt,desc").content()).hasSize(1);
        // Technicians cannot search or sort by customer name, and customer name is never in their projection.
        assertThat(transfers.search(technician, "khách kết quả", null, null, null, 0, 10, "createdAt,desc").content()).isEmpty();
        expect(400, () -> transfers.search(technician, null, null, null, null, 0, 10, "customerName,asc"));
        assertThat(transfers.search(technician, null, null, null, null, 0, 10, "createdAt,desc").content())
                .allSatisfy(t -> assertThat(t.customerName()).isNull());
        expect(400, () -> transfers.search(admin, null, null, null, null, 0, 10, "passwordHash,asc"));
        expect(400, () -> transfers.search(admin, null, null, null, null, 0, 101, "createdAt,desc"));
        expect(400, () -> transfers.search(admin, null, null, LocalDate.now().plusDays(1), LocalDate.now(), 0, 10, "createdAt,desc"));
        assertThat(transfers.search(admin, null, null, LocalDate.now().plusDays(1), null, 0, 10, "createdAt,desc").content()).isEmpty();
        assertThat(transfers.search(admin, null, TransferStatus.ACCEPTED, null, LocalDate.now(), 0, 10, "transferDate,desc").content()).hasSize(1);
        assertThat(transfers.search(admin, null, TransferStatus.PENDING, null, null, 0, 10, "createdAt,desc").content()).isEmpty();
    }

    @Test
    void testingRecordListSortsSearchesAndFiltersByVietnamDate() {
        assertThat(testingRecords.search("CT777", null, null, null, 0, 10, "recordCode,asc").content()).hasSize(1);
        assertThat(testingRecords.search("không-có-gì", null, null, null, 0, 10, "createdAt,desc").content()).isEmpty();
        assertThat(testingRecords.search(null, TestRecordStatus.REVIEWED, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1),
                0, 10, "sampleCode,desc").content()).hasSize(1);
        assertThat(testingRecords.search(null, null, LocalDate.now().plusDays(2), null, 0, 10, "createdAt,desc").content()).isEmpty();
        expect(400, () -> testingRecords.search(null, null, null, null, 0, 10, "hack,asc"));
    }

    @Test
    void readyRecordsListsReviewedRecordsWithoutADossierAndResultListFilters() throws Exception {
        // setUp already created a dossier for the only reviewed record, so nothing is waiting.
        assertThat(workflow.readyRecords()).isEmpty();
        var list = workflow.list(null, "kq-không-có", null, null, 0, 10, "updatedAt,desc");
        assertThat(list.content()).isEmpty();
        assertThat(workflow.list(com.example.app.results.entity.ResultReleaseStatus.DRAFT, "CT777", null, null, 0, 10, "version,asc").content()).hasSize(1);
        assertThat(workflow.list(com.example.app.results.entity.ResultReleaseStatus.PUBLISHED, null, null, null, 0, 10, "updatedAt,desc").content()).isEmpty();
        expect(400, () -> workflow.list(null, null, null, null, 0, 10, "lookupCode,asc"));
    }

    @Test
    void notificationListSearchesAndFiltersByDateAndType() {
        notifications.notifyUsers(List.of(technician), admin,
                com.example.app.notification.entity.StaffNotificationType.TRANSFER_SUBMITTED,
                "Phiếu chuyển mẫu mới", "Có mẫu sầu riêng", "/x", "TRANSFER", 987654L);
        assertThat(notifications.findFor(technician, 0, 10, "createdAt,desc", false, null, null, null, null).content()).isNotEmpty();
        assertThat(notifications.findFor(technician, 0, 10, "title,asc", false, "sầu riêng", null, null, null).content()).hasSize(1);
        assertThat(notifications.findFor(technician, 0, 10, "createdAt,desc", false, "không-khớp", null, null, null).content()).isEmpty();
        assertThat(notifications.findFor(technician, 0, 10, "createdAt,desc", false, null,
                com.example.app.notification.entity.StaffNotificationType.TRANSFER_REJECTED, null, null).content()).isEmpty();
        assertThat(notifications.findFor(technician, 0, 10, "createdAt,desc", false, null, null, LocalDate.now().plusDays(1), null).content()).isEmpty();
        // Another user never sees it, whatever the filter.
        assertThat(notifications.findFor(otherTechnician, 0, 10, "createdAt,desc", false, "Có mẫu sầu riêng", null, null, null).content()).isEmpty();
        expect(400, () -> notifications.findFor(technician, 0, 10, "message,asc", false, null, null, null, null));
    }

    @Test
    void staffListSearchesFiltersAndSortsOnTheServer() {
        var found = userService.search("ktv", Role.TECHNICIAN, true, 0, 10, "displayName,asc");
        assertThat(found.content()).extracting(u -> u.displayName()).containsExactly("KTV khác", "KTV lập hồ sơ");
        assertThat(userService.search("wf-admin", null, null, 0, 10, "email,desc").content()).hasSize(1);
        assertThat(userService.search(null, Role.ADMIN, false, 0, 10, "createdAt,desc").content()).isEmpty();
        expect(400, () -> userService.search(null, null, null, 0, 10, "passwordHash,asc"));
    }

    // ---- Admin may prepare a dossier too, but never approves their own ----

    @Test
    void adminCanPrepareADossierButAnotherAdminMustApproveIt() {
        // Hand the dossier created in setUp to the admin, as if the admin had opened it.
        entityManager.flush();
        jdbc.update("update result_releases set created_by_user_id = ? where id = ?", admin.getId(), releaseId);
        entityManager.clear();
        User secondAdmin = users.save(new User("wf-admin2@test.local", "Admin thứ hai", "unused", Role.ADMIN));

        var rows = List.of(new ResultReleaseService.ReportRowInput("Cd", "µg/kg", "PP", "KPH", null));
        assertThat(workflow.updateReport(releaseId, rows, "A", "B", admin).items()).hasSize(1);
        expect(403, () -> workflow.updateReport(releaseId, rows, "A", "B", technician)); // not the creator
        assertThat(workflow.submit(releaseId, admin).status()).isEqualTo("SUBMITTED");

        expect(403, () -> workflow.publish(releaseId, null, admin));        // creator cannot approve own dossier
        expect(403, () -> workflow.giveBack(releaseId, "Tự trả", admin));
        assertThat(workflow.publish(releaseId, null, secondAdmin).status()).isEqualTo("PUBLISHED");
    }

    @Test
    void detailOffersPhieu5ReferenceAndSuggestedRowsUntilTheReportIsSaved() {
        var empty = workflow.detail(releaseId);
        assertThat(empty.items()).isEmpty();
        assertThat(empty.reference().recordCode()).startsWith("BBTN-");
        assertThat(empty.reference().measurements()).hasSize(1);
        assertThat(empty.reference().testMethod()).isEqualTo("PP");
        // "Cadimi" is the requested parameter on the transfer; it becomes the first suggested row with the method filled in.
        assertThat(empty.suggestedItems()).extracting(ResultReleaseDetailResponse.ItemResponse::parameter).containsExactly("Cadimi");
        assertThat(empty.suggestedItems().getFirst().method()).isEqualTo("PP");
        fillReport();
        assertThat(workflow.detail(releaseId).suggestedItems()).isEmpty();
    }
}
