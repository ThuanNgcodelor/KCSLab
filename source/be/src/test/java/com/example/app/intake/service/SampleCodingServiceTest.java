package com.example.app.intake.service;

import com.example.app.intake.dto.CreateReceiptRequest;
import com.example.app.intake.dto.ReceiptResponse;
import com.example.app.intake.dto.ReceiptSearchCriteria;
import com.example.app.intake.dto.SampleRequest;
import com.example.app.intake.entity.AcceptanceDecision;
import com.example.app.intake.entity.SampleCodingStatus;
import com.example.app.intake.entity.SealStatus;
import com.example.app.transfer.dto.CreateTransferItemRequest;
import com.example.app.transfer.dto.CreateTransferRequest;
import com.example.app.transfer.dto.TransferDecisionRequest;
import com.example.app.transfer.entity.TransferStatus;
import com.example.app.transfer.service.TransferService;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "app.storage.root=target/test-storage/receipts")
class SampleCodingServiceTest {
    @Autowired private ReceiptService receipts;
    @Autowired private SampleCodingService coding;
    @Autowired private TransferService transfers;
    @Autowired private UserRepository users;
    @Autowired private com.example.app.transfer.receiving.ReceivingService receiving;
    @Autowired private com.example.app.testing.service.TestingRecordService testing;

    private User coder;
    private User verifier;
    private User technician;

    @BeforeEach
    void setUp() {
        coder = users.save(new User("coder@test.local", "Người mã hóa", "unused", Role.RECEPTIONIST));
        verifier = users.save(new User("verifier@test.local", "Người đối chiếu", "unused", Role.RECEPTIONIST));
        technician = users.save(new User("kcs-coding@test.local", "KCS", "unused", Role.TECHNICIAN));
    }

    @Test
    void newSamplesStartUnverifiedWithDistinctSequentialCodes() {
        ReceiptResponse receipt = receipt("0900100001", "CT101", "CT102");

        assertThat(receipt.samples()).extracting(s -> s.codingStatus())
                .containsOnly(SampleCodingStatus.PENDING_CODING);
        List<String> codes = receipt.samples().stream().map(s -> s.sampleCode()).toList();
        assertThat(codes).doesNotHaveDuplicates();
        int first = Integer.parseInt(codes.get(0).substring(codes.get(0).lastIndexOf('-') + 1));
        int second = Integer.parseInt(codes.get(1).substring(codes.get(1).lastIndexOf('-') + 1));
        assertThat(second).isEqualTo(first + 1);
    }

    @Test
    void coderCannotVerifyOwnWorkAndSecondPersonCan() {
        Long sampleId = receipt("0900100002", "CT103").samples().getFirst().id();
        coding.submit(sampleId, coder);

        assertThatThrownBy(() -> coding.verify(sampleId, coder))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
        assertThat(coding.verify(sampleId, verifier).codingStatus()).isEqualTo(SampleCodingStatus.VERIFIED);
        assertThat(coding.history(sampleId)).extracting(e -> e.eventType())
                .containsExactly("SUBMITTED", "VERIFIED");
    }

    @Test
    void verifyBeforeSubmitAndTechnicianAreRejected() {
        Long sampleId = receipt("0900100003", "CT104").samples().getFirst().id();

        assertThatThrownBy(() -> coding.verify(sampleId, verifier))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        assertThatThrownBy(() -> coding.submit(sampleId, technician))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
    }

    @Test
    void rejectedCodeIsCorrectedWithNewCodeAndOldCodeStaysInHistory() {
        var sample = receipt("0900100004", "CT105").samples().getFirst();
        coding.submit(sample.id(), coder);

        assertThatThrownBy(() -> coding.reject(sample.id(), " ", verifier))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        assertThat(coding.reject(sample.id(), "Sai mã nguồn trên nhãn", verifier).codingStatus())
                .isEqualTo(SampleCodingStatus.CORRECTION_REQUIRED);
        assertThatThrownBy(() -> coding.correct(sample.id(), "CT105", null, "Không đổi gì", coder))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(400));

        var corrected = coding.correct(sample.id(), "ct106", null, "Nhập sai mã nguồn", coder);

        assertThat(corrected.sampleCode()).startsWith("CT106-").isNotEqualTo(sample.sampleCode());
        assertThat(corrected.codingStatus()).isEqualTo(SampleCodingStatus.PENDING_CODING);
        var history = coding.history(sample.id());
        assertThat(history).extracting(e -> e.eventType())
                .containsExactly("SUBMITTED", "REJECTED", "CORRECTED");
        assertThat(history.getLast().oldCode()).isEqualTo(sample.sampleCode());
        assertThat(history.getLast().newCode()).isEqualTo(corrected.sampleCode());

        // The retired code is never reissued: a following receipt continues the daily sequence.
        var later = receipt("0900100005", "CT105").samples().getFirst();
        assertThat(later.sampleCode()).isNotEqualTo(sample.sampleCode());
    }

    @Test
    void manuallyCodedSampleCanBeHandedOverWithoutKcsVerification() {
        ReceiptResponse receipt = manuallyCodedReceipt("0900100006", "CT107");
        Long sampleId = receipt.samples().getFirst().id();

        assertThat(searchTransferable()).extracting(ReceiptResponse::id).contains(receipt.id());
        var transfer = transfers.create(transferRequest(receipt.id(), sampleId), coder);
        assertThat(transfers.submit(transfer.id()).status()).isEqualTo(TransferStatus.PENDING);
        assertThat(transfers.decide(transfer.id(), new TransferDecisionRequest(
                TransferStatus.ACCEPTED, null, null, false, null), technician).status())
                .isEqualTo(TransferStatus.ACCEPTED);
        assertThat(coding.find(sampleId).codingStatus()).isEqualTo(SampleCodingStatus.VERIFIED);
    }

    @Test
    void manuallyCodedSampleCanBeHandedOverAndTestedWithoutSecondVerification() {
        var receipt = manuallyCodedReceipt("0900100020", "CT120");
        Long sampleId = receipt.samples().getFirst().id();
        assertThat(coding.find(sampleId).codingStatus()).isEqualTo(SampleCodingStatus.VERIFIED);
        assertThat(searchTransferable()).extracting(ReceiptResponse::id).contains(receipt.id());
        var transfer = transfers.create(transferRequest(receipt.id(), sampleId), coder);
        transfers.submit(transfer.id());
        transfers.decide(transfer.id(), new TransferDecisionRequest(TransferStatus.ACCEPTED, null, null, false, null), technician);
        assertThat(testing.candidates()).anyMatch(candidate -> candidate.sampleId().equals(sampleId));
    }

    @Test
    void manuallyCodedSampleDoesNotRequireSecondPersonVerification() {
        User admin = users.save(new User("admin-coder@test.local", "Admin coder", "unused", Role.ADMIN));
        var receipt = manuallyCodedReceipt("0900100021", "CT121");
        Long sampleId = receipt.samples().getFirst().id();
        assertThat(coding.find(sampleId).codingStatus()).isEqualTo(SampleCodingStatus.VERIFIED);
        assertThat(receipt.samples().getFirst().sampleCode()).isEqualTo("CT121-SR-MANUAL");
    }

    @Test
    void correctionIsRejectedOnVerifiedSample() {
        Long sampleId = receipt("0900100007", "CT108").samples().getFirst().id();
        coding.submit(sampleId, coder);
        coding.verify(sampleId, verifier);

        assertThatThrownBy(() -> coding.correct(sampleId, "CT109", null, "Muộn", coder))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
    }

    private List<ReceiptResponse> searchTransferable() {
        return receipts.findAll(new ReceiptSearchCriteria(null, null, null, null, null, null, null, true),
                0, 100, "receivedAt,desc").content();
    }

    private ReceiptResponse manuallyCodedReceipt(String phone, String source) {
        List<SampleRequest> samples = List.of(new SampleRequest(source + "-SR-MANUAL", source, "SR",
                "Mẫu " + source, "1 kg", "Túi PE", "Nguyên vẹn", "Cadimi"));
        return receipts.create(new CreateReceiptRequest(
                "Khách " + phone, phone, "REQ-" + phone, null, "Cần Thơ",
                "Người lấy mẫu", Instant.now().minusSeconds(3600), "Vùng trồng", "Cơ sở đóng gói",
                "65C-TEST", null, null, "Người giao", Instant.now(), "Sầu riêng", "1 kg", "Túi PE",
                null, SealStatus.INTACT, "Nguyên vẹn", null, null, null, "Nhiệt độ phòng", "Cadimi",
                LocalDate.now().plusDays(5), null, AcceptanceDecision.ACCEPTED, null, samples), coder);
    }

    private CreateTransferRequest transferRequest(Long receiptId, Long sampleId) {
        return new CreateTransferRequest(receiptId, "KCS", LocalDate.now(), LocalDate.now().plusDays(3),
                false, null, null, List.of(new CreateTransferItemRequest(
                        sampleId, null, null, null, null, "Phương pháp nội bộ")));
    }

    private ReceiptResponse receipt(String phone, String... sources) {
        List<SampleRequest> samples = java.util.Arrays.stream(sources).map(source -> new SampleRequest(
                source, "SR", "Mẫu " + source, "1 kg", "Túi PE", "Nguyên vẹn", "Cadimi")).toList();
        return receipts.create(new CreateReceiptRequest(
                "Khách " + phone, phone, "REQ-" + phone, null, "Cần Thơ",
                "Người lấy mẫu", Instant.now().minusSeconds(3600), "Vùng trồng", "Cơ sở đóng gói",
                "65C-TEST", null, null, "Người giao", Instant.now(), "Sầu riêng", "1 kg", "Túi PE",
                null, SealStatus.INTACT, "Nguyên vẹn", null, null, null, "Nhiệt độ phòng", "Cadimi",
                LocalDate.now().plusDays(5), null, AcceptanceDecision.ACCEPTED, null, samples), coder);
    }
}
