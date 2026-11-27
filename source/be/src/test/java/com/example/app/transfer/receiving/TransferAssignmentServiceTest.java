package com.example.app.transfer.receiving;

import com.example.app.intake.entity.AcceptanceDecision;
import com.example.app.intake.entity.Customer;
import com.example.app.intake.entity.Receipt;
import com.example.app.intake.entity.Sample;
import com.example.app.intake.repository.CustomerRepository;
import com.example.app.intake.repository.ReceiptRepository;
import com.example.app.notification.service.StaffNotificationService;
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
class TransferAssignmentServiceTest {
    @Autowired private TransferAssignmentService assignments;
    @Autowired private TransferService transferService;
    @Autowired private StaffNotificationService notifications;
    @Autowired private UserRepository userRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ReceiptRepository receiptRepository;

    private User receptionist, techA, techB, techC, admin;
    private TransferResponse transfer;

    @BeforeEach
    void setUp() {
        receptionist = userRepository.save(new User("as-rec@test.local", "Tiếp nhận", "unused", Role.RECEPTIONIST));
        techA = userRepository.save(new User("as-a@test.local", "KTV An", "unused", Role.TECHNICIAN));
        techB = userRepository.save(new User("as-b@test.local", "KTV Bình", "unused", Role.TECHNICIAN));
        techC = userRepository.save(new User("as-c@test.local", "KTV Cường", "unused", Role.TECHNICIAN));
        admin = userRepository.save(new User("as-admin@test.local", "Quản trị", "unused", Role.ADMIN));
        Customer customer = customerRepository.save(new Customer("Khách giao việc", "0900000888", null, "Cần Thơ"));
        Receipt receipt = new Receipt("TN-ASSIGN-1", customer, receptionist, "Người giao", Instant.now(), "Mẫu 1", "1", "Túi PE",
                "S1", true, "Nguyên vẹn", null, null, null, "Thường", "Cadimi (Cd)", LocalDate.now().plusDays(4), null,
                AcceptanceDecision.ACCEPTED, null);
        receipt.addSample(new Sample("AS-001", "ĐT001", "Sầu riêng", LocalDate.now(), 1, "Mẫu 1", "1", "Túi PE",
                "Nguyên vẹn", "Cadimi (Cd)", "2kg").markLegacyVerified());
        receipt.setRequestNumber("CFC26.07001");
        receipt = receiptRepository.save(receipt);
        Sample sample = receipt.getSamples().get(0);
        transfer = transferService.create(new CreateTransferRequest(receipt.getId(), "KCS", LocalDate.now(), LocalDate.now().plusDays(4),
                false, null, null, List.of(new CreateTransferItemRequest(sample.getId(), "2kg", "1", "Nguyên vẹn", "Cadimi (Cd)", "CFC-TN-17-2026"))), receptionist);
    }

    @Test
    void cannotTagWhileDraft() {
        assertThatThrownBy(() -> assignments.assign(transfer.id(), List.of(techB.getId()), techA))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("chờ nhận hoặc đã nhận");
    }

    @Test
    void taggingManyTechniciansNotifiesOnlyTheNewOnesAndNeverTheActor() {
        transferService.submit(transfer.id(), receptionist);
        long beforeB = notifications.findFor(techB, 0, 50, null, false).totalElements();
        var result = assignments.assign(transfer.id(), List.of(techA.getId(), techB.getId(), techC.getId()), techA);
        assertThat(result).extracting(ReceivingTransferResponse.Assignee::displayName).containsExactly("KTV An", "KTV Bình", "KTV Cường");

        var bNotices = notifications.findFor(techB, 0, 50, null, false);
        assertThat(bNotices.totalElements()).isEqualTo(beforeB + 1);
        assertThat(bNotices.content().get(0).message()).contains("KTV An", "KTV Bình", "KTV Cường", "CFC26.07001");
        assertThat(bNotices.content().get(0).targetUrl()).isEqualTo("/admin/receiving/transfers/" + transfer.id());
        assertThat(notifications.findFor(techA, 0, 50, null, false).content())
                .noneMatch(notice -> notice.message().contains("thử nghiệm 1 mẫu"));

        // Saving the same set again must not notify anybody twice.
        assignments.assign(transfer.id(), List.of(techA.getId(), techB.getId(), techC.getId()), techA);
        assertThat(notifications.findFor(techB, 0, 50, null, false).totalElements()).isEqualTo(beforeB + 1);

        // Untag one person.
        assertThat(assignments.assign(transfer.id(), List.of(techB.getId()), admin))
                .extracting(ReceivingTransferResponse.Assignee::displayName).containsExactly("KTV Bình");
    }

    @Test
    void onlyActiveTechniciansCanBeTaggedAndOnlyStaffWithAccessCanTag() {
        transferService.submit(transfer.id(), receptionist);
        assertThatThrownBy(() -> assignments.assign(transfer.id(), List.of(receptionist.getId()), techA))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("không phải kỹ thuật viên");
        assertThatThrownBy(() -> assignments.assign(transfer.id(), List.of(techB.getId()), receptionist))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> assignments.assign(transfer.id(), List.of(999999L), techA))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(assignments.technicians()).extracting(ReceivingTransferResponse.Assignee::displayName)
                .contains("KTV An", "KTV Bình", "KTV Cường").doesNotContain("Tiếp nhận", "Quản trị");
    }
}
