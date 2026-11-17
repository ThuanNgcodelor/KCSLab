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
    @Autowired private EntityManager entityManager;

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

        assertThat(workflow.submit(releaseId, technician).status()).isEqualTo("SUBMITTED");
        var published = workflow.publish(releaseId, null, admin);

        assertThat(published.status()).isEqualTo("PUBLISHED");
        assertThat(published.lookupCode()).hasSize(16).matches("[A-Z2-9]+");
        assertThat(published.resultNumber()).startsWith("KQ-");
        // Default: only the PDF may be downloaded publicly; Word needs an explicit Admin choice.
        assertThat(published.files()).filteredOn(ResultReleaseDetailResponse.FileResponse::publicDownload)
                .extracting(ResultReleaseDetailResponse.FileResponse::originalName).containsExactly("kq.pdf");
        expect(409, () -> workflow.addFiles(releaseId, List.of(pdf("late.pdf")), technician));

        var revoked = workflow.revoke(releaseId, "Phát hiện sai số liệu", admin);
        assertThat(revoked.status()).isEqualTo("REVOKED");
        var next = workflow.revise(releaseId, technician);
        assertThat(next.version()).isEqualTo(2);
        assertThat(next.status()).isEqualTo("DRAFT");
        expect(409, () -> workflow.revise(releaseId, technician));

        assertThat(workflow.history(releaseId)).extracting(ResultReleaseDetailResponse.EventResponse::eventType)
                .containsExactly("CREATED", "FILE_ADDED", "FILE_ADDED", "SUBMITTED", "PUBLISHED", "REVOKED");
    }

    @Test
    void submitNeedsFilesAndOnlyTheCreatingTechnicianMayUploadOrSubmit() throws Exception {
        expect(409, () -> workflow.submit(releaseId, technician));
        expect(403, () -> workflow.addFiles(releaseId, List.of(pdf("a.pdf")), otherTechnician));
        expect(403, () -> workflow.addFiles(releaseId, List.of(pdf("a.pdf")), admin));
        workflow.addFiles(releaseId, List.of(pdf("a.pdf")), technician);
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
        workflow.addFiles(releaseId, List.of(pdf("a.pdf")), technician);
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
    void adminCanExplicitlyEnableWordAndMustKeepAtLeastOnePublicFile() throws Exception {
        var detail = workflow.addFiles(releaseId, List.of(pdf("a.pdf"), docx("b.docx")), technician);
        Long pdfId = detail.files().get(0).id();
        Long docxId = detail.files().get(1).id();
        workflow.submit(releaseId, technician);

        expect(400, () -> workflow.publish(releaseId, Set.of(999999L), admin));
        expect(409, () -> workflow.publish(releaseId, Set.of(), admin));
        var published = workflow.publish(releaseId, Set.of(pdfId, docxId), admin);
        assertThat(published.files()).allMatch(ResultReleaseDetailResponse.FileResponse::publicDownload);
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
}
