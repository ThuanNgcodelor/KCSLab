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
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void paginatesWithTwentyByDefaultContractAndWhitelistedSorts() {
        User receptionist = userRepository.save(new User(
                "pagination@test.local", "Nhan vien phan trang", "unused", Role.RECEPTIONIST));
        Instant now = Instant.now();
        receiptService.create(receiptRequest("Beta Farm", "0900000102", now.minusSeconds(120)), receptionist);
        receiptService.create(receiptRequest("Alpha Farm", "0900000101", now.minusSeconds(60)), receptionist);
        receiptService.create(receiptRequest("Gamma Farm", "0900000103", now), receptionist);

        var firstPage = receiptService.findAll(0, 2, "customerName,asc");
        var secondPage = receiptService.findAll(1, 2, "customerName,asc");

        assertThat(firstPage.content()).extracting(ReceiptResponse::customerName)
                .containsExactly("Alpha Farm", "Beta Farm");
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.first()).isTrue();
        assertThat(firstPage.last()).isFalse();
        assertThat(firstPage.sort()).isEqualTo("customerName,asc");
        assertThat(secondPage.content()).extracting(ReceiptResponse::customerName)
                .containsExactly("Gamma Farm");
        assertThat(secondPage.last()).isTrue();

        assertThatThrownBy(() -> receiptService.findAll(0, 20, "passwordHash,asc"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Trường sort không hợp lệ");
        assertThatThrownBy(() -> receiptService.findAll(0, 101, "receivedAt,desc"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Kích thước trang");
    }

    private CreateReceiptRequest receiptRequest(String customerName, String phone, Instant receivedAt) {
        return new CreateReceiptRequest(
                customerName, phone, null, "Can Tho",
                "Nguoi lay mau", receivedAt.minusSeconds(3600), "Vung trong test",
                "Co so dong goi test", "65C-TEST", null, null,
                "Nguoi giao", receivedAt, "Sau rieng test", "1 kg", "Tui PE",
                null, true, "Nguyen ven", null, null, null,
                "Nhiet do phong", "Cadimi", LocalDate.now().plusDays(5), null,
                AcceptanceDecision.ACCEPTED, null,
                List.of(new SampleRequest(
                        "CT901", "SR", "Mau " + customerName, "1 kg", "Tui PE",
                        "Nguyen ven", "Cadimi")));
    }
}
