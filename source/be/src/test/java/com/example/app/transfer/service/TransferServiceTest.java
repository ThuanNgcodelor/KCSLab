package com.example.app.transfer.service;

import com.example.app.intake.entity.AcceptanceDecision;
import com.example.app.intake.entity.Customer;
import com.example.app.intake.entity.Receipt;
import com.example.app.intake.entity.ReceiptStatus;
import com.example.app.intake.entity.Sample;
import com.example.app.intake.repository.CustomerRepository;
import com.example.app.intake.repository.ReceiptRepository;
import com.example.app.transfer.dto.CreateTransferItemRequest;
import com.example.app.transfer.dto.CreateTransferRequest;
import com.example.app.transfer.dto.TransferDecisionRequest;
import com.example.app.transfer.dto.TransferResponse;
import com.example.app.transfer.entity.TransferStatus;
import com.example.app.transfer.receiving.ReceivingService;
import com.example.app.transfer.repository.SampleTransferRepository;
import jakarta.persistence.EntityManager;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TransferServiceTest {
    @Autowired private TransferService transferService;
    @Autowired private UserRepository userRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ReceiptRepository receiptRepository;
    @Autowired private ReceivingService receivingService;
    @Autowired private EntityManager entityManager;
    @Autowired private SampleTransferRepository transferRepository;

    private User receptionist;
    private User technician;
    private Receipt receipt;
    private Sample sample;

    @BeforeEach
    void setUp() {
        receptionist = userRepository.save(new User(
                "receptionist@test.local", "Nhân viên tiếp nhận", "unused", Role.RECEPTIONIST));
        technician = userRepository.save(new User(
                "technician@test.local", "Nhân viên KCS", "unused", Role.TECHNICIAN));
        Customer customer = customerRepository.save(new Customer(
                "Khách hàng kiểm thử", "0900000001", null, "Đắk Lắk"));

        receipt = new Receipt(
                "TN-TEST-001", customer, receptionist, "Người giao mẫu", Instant.now(),
                "Sầu riêng", "2 quả", "Thùng carton", "SEAL-01", true,
                "Nguyên vẹn", null, "Xe lạnh", 18.5, "Nhiệt độ phòng",
                "Cadimi, dư lượng thuốc BVTV", LocalDate.now().plusDays(4), null,
                AcceptanceDecision.ACCEPTED, null);
        sample = new Sample(
                "SM-TEST-001", "VT-01", "Sầu riêng", LocalDate.now(), 1,
                "Mẫu cơm sầu riêng", "500 g", "Túi PE", "Nguyên vẹn",
                "Cadimi, dư lượng thuốc BVTV").markLegacyVerified();
        receipt.addSample(sample);
        receipt = receiptRepository.save(receipt);
        sample = receipt.getSamples().getFirst();
    }

    @Test
    void createsDraftSubmitsAndAcceptsIt() {
        TransferResponse created = transferService.create(createRequest(), receptionist);

        assertThat(created.status()).isEqualTo(TransferStatus.DRAFT);
        assertThat(created.items()).hasSize(1);
        assertThat(sample.getStatus()).isEqualTo(ReceiptStatus.RECEIVED);

        TransferResponse submitted = transferService.submit(created.id(), receptionist);
        assertThat(submitted.status()).isEqualTo(TransferStatus.PENDING);
        assertThat(sample.getStatus()).isEqualTo(ReceiptStatus.TRANSFER_PENDING);

        TransferResponse accepted = transferService.decide(created.id(),
                new TransferDecisionRequest(TransferStatus.ACCEPTED, "Đủ mẫu", "Đồng ý phân tích",
                        true, LocalDate.now().plusDays(14)), technician);

        assertThat(accepted.status()).isEqualTo(TransferStatus.ACCEPTED);
        assertThat(accepted.receivedBy()).isEqualTo("Nhân viên KCS");
        assertThat(sample.getStatus()).isEqualTo(ReceiptStatus.TRANSFERRED);
        assertThat(receipt.getStatus()).isEqualTo(ReceiptStatus.TRANSFERRED);
    }

    @Test
    void requiresReasonWhenRejectingTransfer() {
        TransferResponse created = transferService.create(createRequest(), receptionist);
        transferService.submit(created.id(), receptionist);

        assertThatThrownBy(() -> transferService.decide(created.id(),
                new TransferDecisionRequest(TransferStatus.REJECTED, " ", null,
                        false, null), technician))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cần ghi lý do");
    }

    @Test
    void rejectsASecondDecisionAfterTheTransferHasAlreadyBeenAccepted() {
        TransferResponse created = transferService.create(createRequest(), receptionist);
        transferService.submit(created.id(), receptionist);
        TransferDecisionRequest decision = new TransferDecisionRequest(
                TransferStatus.ACCEPTED, "Đủ mẫu", null, false, null);
        transferService.decide(created.id(), decision, technician);

        assertThatThrownBy(() -> transferService.decide(created.id(), decision, technician))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(409));
    }

    @Test
    void receivingDashboardAndSearchUseSubmittedTransfers() {
        TransferResponse created = transferService.create(createRequest(), receptionist);
        transferService.submit(created.id(), receptionist);
        entityManager.flush();
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        var dashboard = receivingService.dashboard(technician, today, today);
        assertThat(dashboard.submittedTransfers()).isEqualTo(1);
        assertThat(dashboard.submittedSamples()).isEqualTo(1);
        assertThat(dashboard.pendingNow()).isEqualTo(1);
        assertThat(dashboard.trend()).singleElement().satisfies(day -> assertThat(day.submitted()).isEqualTo(1));
        var result = receivingService.list(technician, 0, 20, TransferStatus.PENDING,
                sample.getSampleCode(), today, today, null);
        assertThat(result.content()).extracting(item -> item.id()).containsExactly(created.id());
        assertThatThrownBy(() -> receivingService.list(technician, 0, 20, null, null, null, null, "customerName,asc"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void receivingDashboardUsesVietnamMidnightBoundaries() {
        TransferResponse created = transferService.create(createRequest(), receptionist);
        transferService.submit(created.id(), receptionist);
        var transfer = transferRepository.findById(created.id()).orElseThrow();
        ReflectionTestUtils.setField(transfer, "submittedAt", Instant.parse("2026-11-15T17:00:00Z"));
        entityManager.flush();
        assertThat(receivingService.dashboard(technician, LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 16))
                .submittedTransfers()).isEqualTo(1);
        assertThat(receivingService.dashboard(technician, LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 15))
                .submittedTransfers()).isZero();
        var trend = receivingService.dashboard(technician, LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 16)).trend();
        assertThat(trend).hasSize(2);
        assertThat(trend.get(0).submitted()).isZero();
        assertThat(trend.get(1).submitted()).isEqualTo(1);
    }

    @Test
    void receivingDashboardAndPaginationIncludeMoreThanOnePage() {
        for (int i = 0; i < 21; i++) {
            Sample item = new Sample("SM-PAGE-" + i, "VT-01", "Sầu riêng", LocalDate.now(), i + 10,
                    "Mẫu " + i, "1", "Túi", "Tốt", "Cd").markLegacyVerified();
            receipt.addSample(item);
            entityManager.persist(item);
            entityManager.flush();
            var request = new CreateTransferRequest(receipt.getId(), "KCS", LocalDate.now(),
                    LocalDate.now().plusDays(4), false, null, null,
                    List.of(new CreateTransferItemRequest(item.getId(), "500 g", "1 túi", "Tốt", "Cd", "TCVN")));
            transferService.submit(transferService.create(request, receptionist).id(), receptionist);
        }
        entityManager.flush();
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        var first = receivingService.list(technician, 0, 20, TransferStatus.PENDING, null, today, today, "submittedAt,asc");
        var second = receivingService.list(technician, 1, 20, TransferStatus.PENDING, null, today, today, "submittedAt,asc");
        assertThat(first.content()).hasSize(20);
        assertThat(second.content()).hasSize(1);
        assertThat(first.content()).extracting(item -> item.id()).doesNotContain(second.content().getFirst().id());
        assertThat(receivingService.dashboard(technician, today, today).submittedTransfers()).isEqualTo(21);
        assertThat(receivingService.dashboard(technician, today, today).submittedSamples()).isEqualTo(21);
    }

    @Test
    void receivingDashboardUsesVietnamMidnightForDecisions() {
        var created = transferService.create(createRequest(), receptionist);
        transferService.submit(created.id(), receptionist);
        transferService.decide(created.id(), new TransferDecisionRequest(TransferStatus.ACCEPTED, "Đủ mẫu", null, false, null), technician);
        var transfer = transferRepository.findById(created.id()).orElseThrow();
        ReflectionTestUtils.setField(transfer, "receivedAt", Instant.parse("2026-11-15T17:00:00Z"));
        entityManager.flush();
        assertThat(receivingService.dashboard(technician, LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 15)).acceptedTransfers()).isZero();
        assertThat(receivingService.dashboard(technician, LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 16)).acceptedTransfers()).isEqualTo(1);
    }

    @Test
    void allowsCreatingANewTransferAfterRejection() {
        TransferResponse first = transferService.create(createRequest(), receptionist);
        transferService.submit(first.id(), receptionist);
        transferService.decide(first.id(),
                new TransferDecisionRequest(TransferStatus.REJECTED, "Bao bì cần đóng lại", null,
                        false, null), technician);

        assertThat(sample.getStatus()).isEqualTo(ReceiptStatus.RECEIVED);
        TransferResponse second = transferService.create(createRequest(), receptionist);

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(second.status()).isEqualTo(TransferStatus.DRAFT);
    }

    @Test
    void hidesDraftTransfersFromTechniciansUntilSubmitted() {
        TransferResponse created = transferService.create(createRequest(), receptionist);

        assertThat(transferService.findAll(receptionist))
                .extracting(TransferResponse::id)
                .contains(created.id());
        assertThat(transferService.findAll(technician))
                .extracting(TransferResponse::id)
                .doesNotContain(created.id());
        assertThatThrownBy(() -> transferService.findById(created.id(), technician))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Không tìm thấy phiếu chuyển mẫu");

        transferService.submit(created.id(), receptionist);

        assertThat(transferService.findAll(technician))
                .extracting(TransferResponse::id)
                .contains(created.id());
        assertThat(transferService.findById(created.id(), technician).customerName()).isNull();
        assertThat(transferService.findById(created.id(), receptionist).customerName())
                .isEqualTo("Khách hàng kiểm thử");
    }

    @Test
    void updatesDraftBeforeSubmission() {
        TransferResponse created = transferService.create(createRequest(), receptionist);
        CreateTransferRequest updatedRequest = new CreateTransferRequest(
                receipt.getId(), "Phòng KCS cập nhật", LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(5), false, null, "Đã đối chiếu lại",
                List.of(new CreateTransferItemRequest(
                        sample.getId(), "600 g", "1 túi", "Nguyên vẹn",
                        "Cadimi", "CFC-TN cập nhật")));

        TransferResponse updated = transferService.update(created.id(), updatedRequest);

        assertThat(updated.status()).isEqualTo(TransferStatus.DRAFT);
        assertThat(updated.receivingDepartment()).isEqualTo("KCS");
        assertThat(updated.items()).singleElement().satisfies(item -> {
            assertThat(item.weight()).isEqualTo("600 g");
            assertThat(item.testMethod()).isEqualTo("CFC-TN cập nhật");
        });
        assertThat(sample.getStatus()).isEqualTo(ReceiptStatus.RECEIVED);
    }

    private CreateTransferRequest createRequest() {
        return new CreateTransferRequest(
                receipt.getId(), "Phòng KCS", LocalDate.now(), LocalDate.now().plusDays(4),
                false, null, null,
                List.of(new CreateTransferItemRequest(
                        sample.getId(), "500 g", "1 túi", "Nguyên vẹn",
                        "Cadimi, dư lượng thuốc BVTV", "TCVN/ISO nội bộ")));
    }
}
