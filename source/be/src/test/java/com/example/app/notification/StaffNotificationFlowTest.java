package com.example.app.notification;

import com.example.app.intake.dto.CreateReceiptRequest;
import com.example.app.intake.service.ReceiptService;
import com.example.app.notification.entity.StaffNotification;
import com.example.app.notification.entity.StaffNotificationType;
import com.example.app.notification.repository.StaffNotificationRepository;
import com.example.app.notification.service.StaffNotificationService;
import com.example.app.sampling.dto.TestRequestDtos;
import com.example.app.sampling.service.TestRequestService;
import com.example.app.transfer.dto.CreateTransferItemRequest;
import com.example.app.transfer.dto.CreateTransferRequest;
import com.example.app.transfer.receiving.ReceivingService;
import com.example.app.transfer.service.TransferService;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class StaffNotificationFlowTest {
    @Autowired TestRequestService requests;
    @Autowired ReceiptService receipts;
    @Autowired TransferService transfers;
    @Autowired ReceivingService receiving;
    @Autowired StaffNotificationService notifications;
    @Autowired StaffNotificationRepository rows;
    @Autowired UserRepository users;
    @Autowired ObjectMapper json;
    @Autowired com.example.app.results.service.ResultReleaseService reports;

    private User user(String name, Role role) { return users.save(new User(name + "@flow.test", name, "unused", role)); }

    private TestRequestDtos.SaveRequest request() {
        return json.readValue("""
                {"requestNumber":"CFC88.9001","requestDate":"2026-11-24","customerName":"Khach thu",
                 "customerPhone":"0900000099","sampleName":"Mau thu","samplingMode":"CUSTOMER","completed":false,
                 "items":[{"sampleNameOrId":"Mau 1","parameters":"Cadimi (Cd)","matrix":"Ruột"}]}
                """, TestRequestDtos.SaveRequest.class);
    }

    @Test void requestCreationNotifiesReceptionistsAndOtherAdminsOnceButNotKcsOrTheCreator() {
        var actor = user("creator", Role.ADMIN);
        var receptionist = user("receptionist", Role.RECEPTIONIST);
        var otherAdmin = user("other-admin", Role.ADMIN);
        var technician = user("technician", Role.TECHNICIAN);
        var disabled = user("disabled", Role.RECEPTIONIST);
        ReflectionTestUtils.setField(disabled, "enabled", false); users.saveAndFlush(disabled);
        var created = requests.create(request(), actor);
        requests.update(created.id(), request(), actor);
        var page = notifications.findFor(receptionist, 0, 100, null, false, created.requestNumber(), StaffNotificationType.TEST_REQUEST_CREATED, null, null);
        assertThat(page.content()).singleElement().satisfies(notice -> {
            assertThat(notice.targetUrl()).isEqualTo("/admin/requests/" + created.id());
            assertThat(notice.message()).contains("creator", created.requestNumber());
        });
        assertThat(notifications.findFor(otherAdmin, 0, 100, null, false, created.requestNumber(), StaffNotificationType.TEST_REQUEST_CREATED, null, null).content()).hasSize(1);
        for (var excluded : List.of(actor, technician, disabled)) {
            assertThat(notifications.findFor(excluded, 0, 100, null, false, created.requestNumber(), StaffNotificationType.TEST_REQUEST_CREATED, null, null).content()).isEmpty();
        }
    }

    @Test void historicalKcsNoticesHideCompanyInListReadResponseAndSearch() {
        var actor = user("old-receptionist", Role.RECEPTIONIST);
        var technician = user("old-technician", Role.TECHNICIAN);
        var old = rows.saveAndFlush(new StaffNotification(technician, actor, StaffNotificationType.INTAKE_RECORDED,
                "Phiếu tiếp nhận mới", "old-receptionist đã ghi nhận 2 mẫu từ PRIVATE-COMPANY-TEST", "/admin/intake/99", "RECEIPT", 99L, "legacy-test"));
        var visible = notifications.findFor(technician, 0, 20, null, false, "ghi nhận 2 mẫu", null, null, null);
        assertThat(visible.content()).singleElement().satisfies(notice -> {
            assertThat(notice.message()).isEqualTo("old-receptionist đã ghi nhận 2 mẫu");
            assertThat(notice.targetUrl()).isEqualTo("/admin/receiving");
        });
        assertThat(notifications.findFor(technician, 0, 20, null, false, "PRIVATE-COMPANY-TEST", null, null, null).content()).isEmpty();
        assertThat(notifications.markRead(old.getId(), technician).message()).doesNotContain("PRIVATE-COMPANY-TEST");
    }

    @Test void weightAndCountRemainSeparateFromIntakeThroughTransferAndKcsAndNoticeIdentifiesRequest() {
        var receptionist = user("sender", Role.RECEPTIONIST);
        var technician = user("receiver", Role.TECHNICIAN);
        var admin = user("admin", Role.ADMIN);
        var request = json.readValue("""
                {"customerName":"PRIVATE-COMPANY-TEST","customerPhone":"0900000099","requestNumber":"CFC88.9002",
                 "deliveredBy":"Nguoi giao thu","sampleName":"Mau thu","quantity":"2 quả","sealStatus":"INTACT",
                 "sampleCondition":"Nguyên vẹn","acceptanceDecision":"ACCEPTED",
                 "samples":[{"sampleCode":"TEST-NOTIFY-WEIGHT-001","sourceCode":"CT901","productType":"SR","name":"Mau thu",
                   "quantity":"2 quả","weight":"500 g","packaging":"Túi PE","condition":"Nguyên vẹn","requestedParameters":"Cadimi"}]}
                """, CreateReceiptRequest.class);
        var receipt = receipts.create(request, receptionist);
        assertThat(receipts.findById(receipt.id()).samples().getFirst().quantity()).isEqualTo("2 quả");
        assertThat(receipts.findById(receipt.id()).samples().getFirst().weight()).isEqualTo("500 g");
        var kcsNotice = notifications.findFor(technician, 0, 100, null, false, receipt.requestNumber(), StaffNotificationType.INTAKE_RECORDED, null, null).content();
        assertThat(kcsNotice).singleElement().satisfies(notice -> {
            assertThat(notice.message()).contains("sender", receipt.requestNumber()).doesNotContain("PRIVATE-COMPANY-TEST");
            assertThat(notice.targetUrl()).isEqualTo("/admin/receiving");
        });
        assertThat(notifications.findFor(admin, 0, 100, null, false, "PRIVATE-COMPANY-TEST", StaffNotificationType.INTAKE_RECORDED, null, null).content()).hasSize(1);
        long sampleId = receipt.samples().getFirst().id();
        var transfer = transfers.create(new CreateTransferRequest(receipt.id(), "KCS", LocalDate.now(), LocalDate.now().plusDays(3),
                false, null, null, List.of(new CreateTransferItemRequest(sampleId, null, null, null, null, "Phuong phap thu"))), receptionist);
        assertThat(transfer.items().getFirst().quantity()).isEqualTo("2 quả");
        assertThat(transfer.items().getFirst().weight()).isEqualTo("500 g");
        transfers.submit(transfer.id(), receptionist);
        var kcs = receiving.detail(transfer.id(), technician);
        assertThat(kcs.items().getFirst().weight()).isEqualTo("500 g");
        assertThat(kcs.items().getFirst().quantity()).isEqualTo("2 quả");
        assertThat(reports.header(transfer.items().getFirst().id()).sampleQuantity()).isEqualTo("2 quả / 500 g");
        assertThat(notifications.findFor(technician, 0, 100, null, false, receipt.requestNumber(), StaffNotificationType.TRANSFER_SUBMITTED, null, null).content())
                .singleElement().satisfies(notice -> {
                    assertThat(notice.message()).contains("sender đã chuyển 1 mẫu", "Số/No. CFC88.9002").doesNotContain("PRIVATE-COMPANY-TEST");
                    assertThat(notice.targetUrl()).isEqualTo("/admin/receiving/transfers/" + transfer.id());
                });
    }
}
