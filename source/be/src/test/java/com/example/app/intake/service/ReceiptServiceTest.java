package com.example.app.intake.service;

import com.example.app.intake.dto.CreateReceiptRequest;
import com.example.app.intake.dto.ReceiptAttachmentResponse;
import com.example.app.intake.dto.ReceiptResponse;
import com.example.app.intake.dto.ReceiptSearchCriteria;
import com.example.app.intake.dto.SampleRequest;
import com.example.app.intake.entity.AcceptanceDecision;
import com.example.app.intake.entity.ReceiptAttachmentCategory;
import com.example.app.intake.entity.SealStatus;
import com.example.app.intake.repository.SampleRepository;
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
    @Autowired private SampleRepository sampleRepository;

    @Test
    void createsMultiSampleSourceRecordAndReturnsPersistedAttachmentMetadata() {
        User receptionist = userRepository.save(new User(
                "intake@test.local", "Nhan vien tiep nhan", "unused", Role.RECEPTIONIST));
        CreateReceiptRequest request = new CreateReceiptRequest(
                "Khach hang test", "0900000099", "REQ-TEST-001", null, "Can Tho",
                "Nguoi lay mau", Instant.now().minusSeconds(3600), "Vung trong CT-TEST",
                "Co so dong goi test", "65C-TEST", "MOOC-TEST", "CONT-TEST",
                "Nguoi giao", Instant.now(), "Sau rieng test", "2 kg", "Tui PE",
                "SEAL-TEST", SealStatus.INTACT, "Nguyen ven", null, "Xe lanh", 18.5,
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

    @Test
    void searchesOnlyReceiptsThatStillHaveTransferableSamples() {
        User receptionist = userRepository.save(new User(
                "transfer-search@test.local", "Nhan vien chuyen mau", "unused", Role.RECEPTIONIST));
        Instant now = Instant.now();
        ReceiptResponse unavailable = receiptService.create(
                receiptRequest("Alpha Unavailable", "0900000201", now.minusSeconds(60)), receptionist);
        ReceiptResponse available = receiptService.create(
                receiptRequest("Beta Available", "0900000202", now), receptionist);
        var availableSample = sampleRepository.findById(available.samples().getFirst().id()).orElseThrow();
        availableSample.markLegacyVerified();
        sampleRepository.saveAndFlush(availableSample);

        var sample = sampleRepository.findById(unavailable.samples().getFirst().id()).orElseThrow();
        sample.markTransferPending();
        sampleRepository.saveAndFlush(sample);

        var result = receiptService.findAll(new ReceiptSearchCriteria(
                null, null, null, null, null, null, null, true),
                0, 20, "receivedAt,desc");

        assertThat(result.content()).extracting(ReceiptResponse::customerName)
                .containsExactly("Beta Available");
    }

    @Test
    void requestNumberIdentifiesExactlyOneReceipt() {
        User receptionist = userRepository.save(new User(
                "dup@test.local", "Nhan vien trung so", "unused", Role.RECEPTIONIST));
        receiptService.create(receiptRequest("DupA", "0900000101", Instant.now()), receptionist);
        // Same Số/No. (different customer, different case/spacing) must be rejected.
        CreateReceiptRequest second = receiptRequest("DupA", "0900000102", Instant.now());
        assertThatThrownBy(() -> receiptService.create(second, receptionist))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("đã có hồ sơ tiếp nhận");
        assertThat(receiptService.create(receiptRequest("DupB", "0900000103", Instant.now()), receptionist).requestNumber())
                .isEqualTo("REQ-DupB");
    }

    @Test
    void cfcRequestNumberIsStoredInCanonicalFiveDigitForm() {
        User receptionist = userRepository.save(new User(
                "canon@test.local", "Nhan vien so chuan", "unused", Role.RECEPTIONIST));
        ReceiptResponse first = receiptService.create(receiptRequest("Canon", "0900000111", Instant.now(), " cfc92.0005 "), receptionist);
        assertThat(first.requestNumber()).isEqualTo("CFC92.00005");
        assertThatThrownBy(() -> receiptService.create(receiptRequest("Canon2", "0900000112", Instant.now(), "CFC92.00005"), receptionist))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("đã có hồ sơ tiếp nhận");
    }

    private CreateReceiptRequest receiptRequest(String customerName, String phone, Instant receivedAt) {
        return receiptRequest(customerName, phone, receivedAt, "REQ-" + customerName);
    }

    private CreateReceiptRequest receiptRequest(String customerName, String phone, Instant receivedAt, String requestNumber) {
        return new CreateReceiptRequest(
                customerName, phone, requestNumber, null, "Can Tho",
                "Nguoi lay mau", receivedAt.minusSeconds(3600), "Vung trong test",
                "Co so dong goi test", "65C-TEST", null, null,
                "Nguoi giao", receivedAt, "Sau rieng test", "1 kg", "Tui PE",
                null, SealStatus.INTACT, "Nguyen ven", null, null, null,
                "Nhiet do phong", "Cadimi", LocalDate.now().plusDays(5), null,
                AcceptanceDecision.ACCEPTED, null,
                List.of(new SampleRequest(
                        "CT901", "SR", "Mau " + customerName, "1 kg", "Tui PE",
                        "Nguyen ven", "Cadimi")));
    }
}
