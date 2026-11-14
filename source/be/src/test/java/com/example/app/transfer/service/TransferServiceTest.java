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
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
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
                "Cadimi, dư lượng thuốc BVTV");
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

        TransferResponse submitted = transferService.submit(created.id());
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
        transferService.submit(created.id());

        assertThatThrownBy(() -> transferService.decide(created.id(),
                new TransferDecisionRequest(TransferStatus.REJECTED, " ", null,
                        false, null), technician))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cần ghi lý do");
    }

    @Test
    void allowsCreatingANewTransferAfterRejection() {
        TransferResponse first = transferService.create(createRequest(), receptionist);
        transferService.submit(first.id());
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

        transferService.submit(created.id());

        assertThat(transferService.findAll(technician))
                .extracting(TransferResponse::id)
                .contains(created.id());
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
        assertThat(updated.receivingDepartment()).isEqualTo("Phòng KCS cập nhật");
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
