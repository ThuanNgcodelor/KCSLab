package com.example.app.intake.service;

import com.example.app.intake.dto.CreateReceiptRequest;
import com.example.app.intake.dto.ReceiptAttachmentResponse;
import com.example.app.intake.dto.ReceiptResponse;
import com.example.app.intake.dto.SampleRequest;
import com.example.app.intake.entity.AcceptanceDecision;
import com.example.app.intake.entity.ReceiptAttachmentCategory;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "app.storage.root=target/test-storage/receipts")
class ReceiptServiceTest {
    @Autowired private ReceiptService receiptService;
    @Autowired private UserRepository userRepository;

    @Test
    void createsMultiSampleSourceRecordAndReturnsPersistedAttachmentMetadata() {
        User receptionist = userRepository.save(new User(
                "intake@test.local", "Nhan vien tiep nhan", "unused", Role.RECEPTIONIST));
        CreateReceiptRequest request = new CreateReceiptRequest(
                "Khach hang test", "0900000099", null, "Can Tho",
                "Nguoi lay mau", Instant.now().minusSeconds(3600), "Vung trong CT-TEST",
                "Co so dong goi test", "65C-TEST", "MOOC-TEST", "CONT-TEST",
                "Nguoi giao", Instant.now(), "Sau rieng test", "2 kg", "Tui PE",
                "SEAL-TEST", true, "Nguyen ven", null, "Xe lanh", 18.5,
                "Lanh", "Cadimi", LocalDate.now().plusDays(5), null,
                AcceptanceDecision.ACCEPTED, null,
                List.of(
                        new SampleRequest("CT901", "SR", "Mau 01", "1 kg", "Tui PE", "Nguyen ven", "Cadimi"),
                        new SampleRequest("CT902", "SR", "Mau 02", "1 kg", "Tui PE", "Nguyen ven", "Auramine O")
                ));

        ReceiptResponse receipt = receiptService.create(request, receptionist);
        assertThat(receipt.samples()).hasSize(2);
        assertThat(receipt.receivedByName()).isEqualTo("Nhan vien tiep nhan");
        assertThat(receipt.growingArea()).isEqualTo("Vung trong CT-TEST");

        MockMultipartFile file = new MockMultipartFile(
                "file", "form-3.pdf", "application/pdf", "%PDF-test".getBytes());
        ReceiptAttachmentResponse attachment = receiptService.attach(
                receipt.id(), file, ReceiptAttachmentCategory.FORM_3, "Phieu 3",
                Instant.now(), receptionist);

        assertThat(attachment.id()).isNotNull();
        assertThat(attachment.category()).isEqualTo(ReceiptAttachmentCategory.FORM_3);
        assertThat(attachment.uploadedByName()).isEqualTo("Nhan vien tiep nhan");
        assertThat(attachment.contentUrl()).contains("/attachments/").endsWith("/content");
    }
}
