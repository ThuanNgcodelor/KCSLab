package com.example.app.intake.service;

import com.example.app.intake.dto.ReceiptEditResponse;
import com.example.app.intake.dto.UpdateReceiptRequest;
import com.example.app.intake.dto.UpdateReceiptRequest.SampleEdit;
import com.example.app.intake.entity.AcceptanceDecision;
import com.example.app.intake.entity.Customer;
import com.example.app.intake.entity.Receipt;
import com.example.app.intake.entity.Sample;
import com.example.app.intake.repository.CustomerRepository;
import com.example.app.intake.repository.ReceiptRepository;
import com.example.app.transfer.dto.CreateTransferItemRequest;
import com.example.app.transfer.dto.CreateTransferRequest;
import com.example.app.transfer.dto.TransferResponse;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ReceiptEditServiceTest {
    @Autowired private ReceiptEditService editService;
    @Autowired private TransferService transferService;
    @Autowired private UserRepository userRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ReceiptRepository receiptRepository;

    private User receptionist;
    private User otherReceptionist;
    private User admin;
    private Receipt receipt;
    private Sample first;
    private Sample second;

    @BeforeEach
    void setUp() {
        receptionist = userRepository.save(new User("edit-rec@test.local", "Tiếp nhận A", "unused", Role.RECEPTIONIST));
        otherReceptionist = userRepository.save(new User("edit-rec2@test.local", "Tiếp nhận B", "unused", Role.RECEPTIONIST));
        admin = userRepository.save(new User("edit-admin@test.local", "Quản trị", "unused", Role.ADMIN));
        Customer customer = customerRepository.save(new Customer("Khách sửa", "0900000777", null, "Cần Thơ"));
        receipt = new Receipt("TN-EDIT-001", customer, receptionist, "Người giao", Instant.now(),
                "Mẫu 1", "1", "Túi PE", "SEAL-1", true, "Nguyên vẹn", null, null, null, "Thường",
                "Cadimi (Cd)", LocalDate.now().plusDays(4), null, AcceptanceDecision.ACCEPTED, null);
        receipt.addSample(new Sample("EDIT-001", "ĐT001", "Sầu riêng", LocalDate.now(), 1, "Mẫu 1", "1", "Túi PE",
                "Nguyên vẹn", "Cadimi (Cd)", "2,4kg").markLegacyVerified());
        receipt.addSample(new Sample("EDIT-002", "ĐT001", "Sầu riêng", LocalDate.now(), 2, "Mẫu 1", "1", "Túi PE",
                "Nguyên vẹn", "Auramine O", "1,6kg").markLegacyVerified());
        receipt = receiptRepository.save(receipt);
        first = receipt.getSamples().get(0);
        second = receipt.getSamples().get(1);
    }

    private UpdateReceiptRequest request(String reason, String firstName, String secondName) {
        return new UpdateReceiptRequest(reason, "Người giao", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                List.of(new SampleEdit(first.getId(), firstName, "1", "2,4kg", "Túi PE", "Nguyên vẹn", "Cadimi (Cd)"),
                        new SampleEdit(second.getId(), secondName, "1", "1,6kg", "Túi PE", "Nguyên vẹn", "Auramine O")));
    }

    @Test
    void receptionistRenamesSamplesBeforeTransferAndKeepsCodes() {
        editService.update(receipt.getId(), request(null, "Mẫu 1", "Mẫu 2"), receptionist);
        Receipt saved = receiptRepository.findById(receipt.getId()).orElseThrow();
        assertThat(saved.getSamples()).extracting(Sample::getName).containsExactly("Mẫu 1", "Mẫu 2");
        assertThat(saved.getSamples()).extracting(Sample::getSampleCode).containsExactly("EDIT-001", "EDIT-002");
        assertThat(saved.getSampleName()).isEqualTo("Mẫu 1");
        List<ReceiptEditResponse> history = editService.history(receipt.getId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).summary()).contains("EDIT-002 · tên: Mẫu 1 → Mẫu 2");
        assertThat(history.get(0).afterTransfer()).isFalse();
    }

    @Test
    void rejectsDuplicateNamesAndEmptyChanges() {
        assertThatThrownBy(() -> editService.update(receipt.getId(), request(null, "Mẫu A", " mẫu  a "), receptionist))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("bị trùng");
        assertThatThrownBy(() -> editService.update(receipt.getId(), request(null, "Mẫu 1", "Mẫu 1"), receptionist))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void anyReceptionistMayEditBeforeTransferButNotATechnician() {
        editService.update(receipt.getId(), request(null, "Mẫu 1", "Mẫu 2"), otherReceptionist);
        User technician = userRepository.save(new User("edit-tech@test.local", "KCS", "unused", Role.TECHNICIAN));
        assertThatThrownBy(() -> editService.update(receipt.getId(), request(null, "Mẫu 1", "Mẫu 3"), technician))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("nhân viên tiếp nhận");
    }

    @Test
    void afterTransferOnlyAdminWithReasonAndTheTransferSnapshotFollows() {
        TransferResponse transfer = transferService.create(new CreateTransferRequest(receipt.getId(), "KCS", LocalDate.now(),
                LocalDate.now().plusDays(4), false, null, null,
                List.of(new CreateTransferItemRequest(first.getId(), "2,4kg", "1", "Nguyên vẹn", "Cadimi (Cd)", "CFC-TN-17-2026"),
                        new CreateTransferItemRequest(second.getId(), "1,6kg", "1", "Nguyên vẹn", "Auramine O", "CFC-TN-16-2026"))),
                receptionist);
        transferService.submit(transfer.id(), receptionist);

        assertThatThrownBy(() -> editService.update(receipt.getId(), request("đổi tên", "Mẫu 1", "Mẫu 2"), receptionist))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("đã gửi cho KCS");
        assertThatThrownBy(() -> editService.update(receipt.getId(), request(" ", "Mẫu 1", "Mẫu 2"), admin))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("lý do");

        editService.update(receipt.getId(), request("Nhập nhầm tên mẫu", "Mẫu 1", "Mẫu 2"), admin);
        TransferResponse after = transferService.findById(transfer.id(), admin);
        assertThat(after.items()).extracting(item -> item.sampleName()).containsExactly("Mẫu 1", "Mẫu 2");
        assertThat(editService.history(receipt.getId()).get(0).afterTransfer()).isTrue();
        assertThat(editService.history(receipt.getId()).get(0).reason()).isEqualTo("Nhập nhầm tên mẫu");
    }
}
