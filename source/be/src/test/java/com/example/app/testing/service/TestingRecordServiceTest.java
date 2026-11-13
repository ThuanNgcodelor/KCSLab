package com.example.app.testing.service;

import com.example.app.intake.entity.AcceptanceDecision;
import com.example.app.intake.entity.Customer;
import com.example.app.intake.entity.Receipt;
import com.example.app.intake.entity.ReceiptStatus;
import com.example.app.intake.entity.Sample;
import com.example.app.intake.repository.CustomerRepository;
import com.example.app.intake.repository.ReceiptRepository;
import com.example.app.testing.dto.EquipmentRequest;
import com.example.app.testing.dto.MeasurementRequest;
import com.example.app.testing.dto.ReagentRequest;
import com.example.app.testing.dto.ReviewTestingRecordRequest;
import com.example.app.testing.dto.SaveTestingRecordRequest;
import com.example.app.testing.dto.TestingRecordResponse;
import com.example.app.testing.entity.MeasurementType;
import com.example.app.testing.entity.TestRecordStatus;
import com.example.app.transfer.dto.CreateTransferItemRequest;
import com.example.app.transfer.dto.CreateTransferRequest;
import com.example.app.transfer.dto.TransferDecisionRequest;
import com.example.app.transfer.dto.TransferResponse;
import com.example.app.transfer.entity.TransferStatus;
import com.example.app.transfer.service.TransferService;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TestingRecordServiceTest {
    @Autowired private TestingRecordService testingRecordService;
    @Autowired private TransferService transferService;
    @Autowired private UserRepository userRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ReceiptRepository receiptRepository;

    private User technician;
    private User admin;
    private Sample sample;
    private Long transferItemId;

    @BeforeEach
    void setUp() {
        User receptionist = userRepository.save(new User(
                "test-reception@test.local", "Nhân viên tiếp nhận", "unused", Role.RECEPTIONIST));
        technician = userRepository.save(new User(
                "test-technician@test.local", "Kỹ thuật viên", "unused", Role.TECHNICIAN));
        admin = userRepository.save(new User(
                "test-admin@test.local", "Trưởng phòng kiểm tra", "unused", Role.ADMIN));
        Customer customer = customerRepository.save(new Customer(
                "Khách hàng thử nghiệm", "0900000002", null, "Lâm Đồng"));
        Receipt receipt = new Receipt(
                "TN-TEST-RECORD", customer, receptionist, "Người giao", Instant.now(),
                "Sầu riêng", "1 mẫu", "Túi PE", null, true, "Nguyên vẹn", null,
                "Xe lạnh", 18.0, "Nhiệt độ phòng", "Cadimi", LocalDate.now().plusDays(5),
                null, AcceptanceDecision.ACCEPTED, null);
        sample = new Sample("SM-TEST-RECORD", "VT-02", "Sầu riêng", LocalDate.now(), 2,
                "Mẫu ruột sầu riêng", "500 g", "Túi PE", "Nguyên vẹn", "Cadimi");
        receipt.addSample(sample);
        receipt = receiptRepository.save(receipt);
        sample = receipt.getSamples().getFirst();

        TransferResponse transfer = transferService.create(new CreateTransferRequest(
                receipt.getId(), LocalDate.now(), LocalDate.now().plusDays(5), false, null, null,
                List.of(new CreateTransferItemRequest(sample.getId(), "500 g", "1 mẫu",
                        "Nguyên vẹn", "Cadimi", "CFC-TN-Cd"))), receptionist);
        transfer = transferService.decide(transfer.id(), new TransferDecisionRequest(
                TransferStatus.ACCEPTED, "Đã nhận đủ", "Tiến hành phân tích", false, null), technician);
        transferItemId = transfer.items().getFirst().id();
    }

    @Test
    void createsSubmitsAndReviewsTestingRecord() {
        TestingRecordResponse created = testingRecordService.create(recordRequest(), technician);

        assertThat(created.status()).isEqualTo(TestRecordStatus.DRAFT);
        assertThat(created.equipment()).hasSize(1);
        assertThat(created.measurements()).hasSize(1);
        assertThat(sample.getStatus()).isEqualTo(ReceiptStatus.IN_TESTING);

        TestingRecordResponse submitted = testingRecordService.submit(created.id(), technician);
        assertThat(submitted.status()).isEqualTo(TestRecordStatus.SUBMITTED);
        assertThat(sample.getStatus()).isEqualTo(ReceiptStatus.AWAITING_REVIEW);

        TestingRecordResponse reviewed = testingRecordService.review(created.id(),
                new ReviewTestingRecordRequest(TestRecordStatus.REVIEWED, "Số liệu phù hợp"), admin);
        assertThat(reviewed.status()).isEqualTo(TestRecordStatus.REVIEWED);
        assertThat(reviewed.checkedBy()).isEqualTo("Trưởng phòng kiểm tra");
        assertThat(sample.getStatus()).isEqualTo(ReceiptStatus.TEST_REVIEWED);
    }

    @Test
    void requiresReasonWhenReturningSubmittedRecord() {
        TestingRecordResponse created = testingRecordService.create(recordRequest(), technician);
        testingRecordService.submit(created.id(), technician);

        assertThatThrownBy(() -> testingRecordService.review(created.id(),
                new ReviewTestingRecordRequest(TestRecordStatus.RETURNED, " "), admin))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cần ghi lý do");
    }

    private SaveTestingRecordRequest recordRequest() {
        Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return new SaveTestingRecordRequest(
                transferItemId, start, start.plus(2, ChronoUnit.HOURS),
                "y = 0.998x + 0.002; R² = 0.9995", "C = A × V × F / m",
                new BigDecimal("98.7500"), "QC nằm trong giới hạn kiểm soát", "Mẫu ổn định",
                List.of(new EquipmentRequest("ICP-MS", "SN-001", "Agilent", "TB-ICP-01", LocalDate.now())),
                List.of(new ReagentRequest("Dung dịch chuẩn Cd", "Cd-1000", "Merck",
                        LocalDate.now().minusDays(1), LocalDate.now().plusMonths(6), "2–8°C", "1000 mg/L")),
                List.of(new MeasurementRequest(MeasurementType.RAW, "1", "0.500 g", "50 mL", "1",
                        "0.012 mg/L", "1.20 mg/kg", "1.20 mg/kg", "1.5%")));
    }
}
