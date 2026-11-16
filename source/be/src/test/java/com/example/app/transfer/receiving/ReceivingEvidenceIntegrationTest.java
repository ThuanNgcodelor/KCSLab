package com.example.app.transfer.receiving;

import com.example.app.intake.entity.*;
import com.example.app.intake.repository.CustomerRepository;
import com.example.app.intake.repository.ReceiptRepository;
import com.example.app.intake.service.ReceiptService;
import com.example.app.transfer.dto.CreateTransferItemRequest;
import com.example.app.transfer.dto.CreateTransferRequest;
import com.example.app.transfer.service.TransferService;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "app.storage.root=target/test-storage/receiving-evidence")
class ReceivingEvidenceIntegrationTest {
    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository users;
    @Autowired private CustomerRepository customers;
    @Autowired private ReceiptRepository receipts;
    @Autowired private ReceiptService receiptService;
    @Autowired private TransferService transfers;

    @Test
    void contentRejectsAttachmentFromAnotherReceipt() throws Exception {
        var sender = users.save(new User("evidence-sender@example.test", "Sender", "unused", Role.RECEPTIONIST));
        var viewer = users.save(new User("evidence-kcs@example.test", "KCS", "unused", Role.TECHNICIAN));
        var customer = customers.save(new Customer("Test", "0900000009", null, null));
        Receipt first = receipt("TN-EVIDENCE-A", "SM-EVIDENCE-A", customer, sender);
        Receipt second = receipt("TN-EVIDENCE-B", "SM-EVIDENCE-B", customer, sender);
        var transfer = transfers.create(new CreateTransferRequest(first.getId(), "KCS", LocalDate.now(),
                LocalDate.now().plusDays(1), false, null, null, List.of(new CreateTransferItemRequest(
                first.getSamples().getFirst().getId(), "500 g", "1", "Tốt", "Cd", "TCVN"))), sender);
        transfers.submit(transfer.id());
        var own = receiptService.attach(first.getId(), new MockMultipartFile("file", "a.png", "image/png", new byte[]{1,2,3}),
                ReceiptAttachmentCategory.SAMPLE_PHOTO, null, null, sender);
        var other = receiptService.attach(second.getId(), new MockMultipartFile("file", "b.png", "image/png", new byte[]{4,5,6}),
                ReceiptAttachmentCategory.SAMPLE_PHOTO, null, null, sender);
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var auth = jwt().jwt(token -> token.subject(viewer.getEmail())).authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN"));
        String url = "/api/v1/receiving/transfers/" + transfer.id() + "/evidence/";
        mvc.perform(get(url + own.id() + "/content").with(auth)).andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1,2,3}));
        mvc.perform(get(url + other.id() + "/content").with(auth)).andExpect(status().isNotFound());
    }

    private Receipt receipt(String code, String sampleCode, Customer customer, User sender) {
        var receipt = new Receipt(code, customer, sender, "Test", Instant.now(), "Test", "1", "Túi", null,
                true, "Tốt", null, null, null, "Thường", "Cd", LocalDate.now().plusDays(1), null,
                AcceptanceDecision.ACCEPTED, null);
        receipt.addSample(new Sample(sampleCode, "CT001", "Sầu riêng", LocalDate.now(), 1,
                "Mẫu", "1", "Túi", "Tốt", "Cd"));
        return receipts.saveAndFlush(receipt);
    }
}
