package com.example.app.publicapi;

import com.example.app.intake.entity.AcceptanceDecision;
import com.example.app.intake.entity.Customer;
import com.example.app.intake.entity.Receipt;
import com.example.app.intake.entity.Sample;
import com.example.app.intake.repository.CustomerRepository;
import com.example.app.intake.repository.ReceiptRepository;
import com.example.app.results.dto.ResultReleaseDetailResponse;
import com.example.app.results.service.ResultReleaseDraftService;
import com.example.app.results.service.ResultReleaseService;
import com.example.app.testing.dto.MeasurementRequest;
import com.example.app.testing.dto.ReviewTestingRecordRequest;
import com.example.app.testing.dto.SaveTestingRecordRequest;
import com.example.app.testing.entity.MeasurementType;
import com.example.app.testing.entity.TestRecordStatus;
import com.example.app.testing.service.TestingRecordService;
import com.example.app.transfer.dto.CreateTransferItemRequest;
import com.example.app.transfer.dto.CreateTransferRequest;
import com.example.app.transfer.dto.TransferDecisionRequest;
import com.example.app.transfer.entity.TransferStatus;
import com.example.app.transfer.service.TransferService;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End to end through the real security filter chain: staff publish a dossier, a customer looks it up with the
 * code and last four phone digits. No Authorization header or cookie is ever sent on the public calls.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "app.storage.results-root=target/test-storage/public-results")
class PublicResultFlowTest {
    private static final String PHONE = "0912345678";
    @Autowired private WebApplicationContext context;
    @Autowired private ResultReleaseService workflow;
    @Autowired private ResultReleaseDraftService drafts;
    @Autowired private TestingRecordService records;
    @Autowired private TransferService transfers;
    @Autowired private UserRepository users;
    @Autowired private CustomerRepository customers;
    @Autowired private ReceiptRepository receipts;
    @Autowired private JdbcTemplate jdbc;

    @Autowired private org.springframework.transaction.PlatformTransactionManager txManager;
    private MockMvc mvc;
    private String clientAddress;
    private User technician;
    private User admin;
    private Long releaseId;
    private String code;
    private Long pdfId;        // the generated KQKN report (set when published)
    private Long uploadedPdfId; // a technician attachment, internal by default
    private Long docxId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        // Rate limits are per client; give every test its own address so they cannot affect each other.
        clientAddress = "10." + java.util.concurrent.ThreadLocalRandom.current().nextInt(256) + "." + java.util.concurrent.ThreadLocalRandom.current().nextInt(256) + "." + java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 255);
        User receptionist = users.save(new User("pub-rec@test.local", "Tiếp nhận", "unused", Role.RECEPTIONIST));
        technician = users.save(new User("pub-tech@test.local", "KTV", "unused", Role.TECHNICIAN));
        admin = users.save(new User("pub-admin@test.local", "Admin", "unused", Role.ADMIN));
        Customer customer = customers.save(new Customer("Nguyễn Văn Bí Mật", PHONE, "bi.mat@example.test", "Địa chỉ riêng"));
        Receipt receipt = new Receipt("TN-PUB-001", customer, receptionist, "Người giao", Instant.now(),
                "Sầu riêng", "1", "Túi", null, true, "Nguyên vẹn", null, null, null, "Thường", "Cadimi",
                LocalDate.now().plusDays(5), null, AcceptanceDecision.ACCEPTED, null);
        receipt.addSample(new Sample("CT888-SR-01012030-001", "CT888", "Sầu riêng", LocalDate.now(), 1,
                "Mẫu", "1", "Túi", "Tốt", "Cadimi").markLegacyVerified());
        receipt = receipts.save(receipt);
        var transfer = transfers.create(new CreateTransferRequest(receipt.getId(), "KCS", LocalDate.now(),
                LocalDate.now().plusDays(5), false, null, null, List.of(new CreateTransferItemRequest(
                receipt.getSamples().getFirst().getId(), null, null, null, null, "PP"))), receptionist);
        transfers.submit(transfer.id(), receptionist);
        var accepted = transfers.decide(transfer.id(), new TransferDecisionRequest(
                TransferStatus.ACCEPTED, null, null, false, null), technician);
        Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        var record = records.create(new SaveTestingRecordRequest(accepted.items().getFirst().id(), start,
                start.plus(1, ChronoUnit.HOURS), null, null, new BigDecimal("99"), null, null, List.of(), List.of(),
                List.of(new MeasurementRequest(MeasurementType.RAW, "1", "1 g", "10 mL", "1", "0.1", "1", "1", "1%"))), technician);
        records.submit(record.id(), technician);
        records.review(record.id(), new ReviewTestingRecordRequest(TestRecordStatus.REVIEWED, "Đạt"), admin);
        releaseId = drafts.create(record.id(), technician).id();
        var withFiles = workflow.addFiles(releaseId, List.of(
                new MockMultipartFile("files", "ket-qua.pdf", "application/pdf",
                        "%PDF-1.4\nnoi dung cong khai\n%%EOF\n".getBytes()),
                new MockMultipartFile("files", "du-lieu.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        com.example.app.results.service.ResultFilePolicyTestSupport.docx())), technician);
        uploadedPdfId = withFiles.files().get(0).id();
        docxId = withFiles.files().get(1).id();
        workflow.updateReport(releaseId, List.of(
                new com.example.app.results.service.ResultReleaseService.ReportRowInput("Cadimium (Cd)", "µg/kg", "CFC-02:2026", "< LOQ", "LOQ = 30 µg/kg")),
                "Phan Trường Tiền", "Nguyễn Công Huân", technician);
        workflow.submit(releaseId, technician);
    }

    private ResultReleaseDetailResponse publish() {
        var published = workflow.publish(releaseId, null, admin);
        code = published.lookupCode();
        pdfId = published.files().stream().filter(f -> f.kind().equals("GENERATED_REPORT")).findFirst().orElseThrow().id();
        return published;
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor from() {
        return request -> { request.setRemoteAddr(clientAddress); return request; };
    }

    private static String body(String code, String last4) {
        return "{\"code\":\"" + code + "\",\"phoneLast4\":\"" + last4 + "\"}";
    }

    private org.springframework.test.web.servlet.ResultActions lookup(String code, String last4) throws Exception {
        return mvc.perform(post("/api/v1/public/results/lookup").contentType("application/json").content(body(code, last4))
                .with(request -> { request.setRemoteAddr(clientAddress); return request; }));
    }

    @Test
    void publishedResultIsFoundWithCodeAndLastFourDigitsAndLeaksNoPersonalData() throws Exception {
        publish();
        String pretty = code.substring(0, 4) + "-" + code.substring(4, 8) + " " + code.substring(8).toLowerCase();

        lookup(pretty, "5678").andExpect(status().isOk())
                .andExpect(jsonPath("$.resultNumber").exists())
                .andExpect(jsonPath("$.files.length()").value(1))
                .andExpect(jsonPath("$.files[0].name").value(org.hamcrest.Matchers.startsWith("KQKN-")))
                .andExpect(jsonPath("$.items[0].parameter").value("Cadimium (Cd)"))
                .andExpect(jsonPath("$.items[0].result").value("< LOQ"))
                .andExpect(jsonPath("$.sampleName").value("Sầu riêng"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("X-Robots-Tag", containsString("noindex")))
                .andExpect(content().string(not(containsString("Bí Mật"))))
                .andExpect(content().string(not(containsString("0912345678"))))
                .andExpect(content().string(not(containsString("example.test"))))
                .andExpect(content().string(not(containsString("Địa chỉ riêng"))));
    }

    @Test
    void wrongPhoneWrongCodeDraftAndMalformedInputAllGetTheSameNeutralAnswer() throws Exception {
        // Not published yet: even the right digits must not reveal anything (code does not exist yet).
        lookup("AAAAAAAAAAAAAAAA", "5678").andExpect(status().isNotFound());
        publish();
        String neutral = lookup(code, "0000").andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        String wrongCode = lookup("ZZZZZZZZZZZZZZZZ", "5678").andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        assertThat(wrongCode).isEqualTo(neutral);
        lookup(code, "56").andExpect(status().isBadRequest());
        lookup(code, "abcd").andExpect(status().isBadRequest());
    }

    @Test
    void downloadReverifiesCredentialsAndOnlyServesPublicFiles() throws Exception {
        publish();
        mvc.perform(post("/api/v1/public/results/download").contentType("application/json").with(from())
                        .content("{\"code\":\"" + code + "\",\"phoneLast4\":\"5678\",\"fileId\":" + pdfId + "}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(result -> assertThat(new String(result.getResponse().getContentAsByteArray(), 0, 5)).isEqualTo("%PDF-"));
        // A technician's uploaded PDF is internal unless the Admin lists it at publication.
        mvc.perform(post("/api/v1/public/results/download").contentType("application/json").with(from())
                        .content("{\"code\":\"" + code + "\",\"phoneLast4\":\"5678\",\"fileId\":" + uploadedPdfId + "}"))
                .andExpect(status().isNotFound());
        // Word is not public unless the Admin enabled it at publication.
        mvc.perform(post("/api/v1/public/results/download").contentType("application/json").with(from())
                        .content("{\"code\":\"" + code + "\",\"phoneLast4\":\"5678\",\"fileId\":" + docxId + "}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/public/results/download").contentType("application/json").with(from())
                        .content("{\"code\":\"" + code + "\",\"phoneLast4\":\"1111\",\"fileId\":" + pdfId + "}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void revokedResultDisappearsFromLookupAndDownload() throws Exception {
        publish();
        lookup(code, "5678").andExpect(status().isOk());
        workflow.revoke(releaseId, "Sai số liệu", admin);
        lookup(code, "5678").andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/public/results/download").contentType("application/json").with(from())
                        .content("{\"code\":\"" + code + "\",\"phoneLast4\":\"5678\",\"fileId\":" + pdfId + "}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void repeatedWrongGuessesLockTheCodeEvenForTheCorrectDigits() throws Exception {
        publish();
        for (int i = 0; i < 5; i++) lookup(code, String.format("%04d", i)).andExpect(status().isNotFound());
        lookup(code, "5678").andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
        // Unknown codes behave identically, so the lock is not an existence oracle.
        for (int i = 0; i < 5; i++) lookup("QQQQQQQQQQQQQQQQ", String.format("%04d", i)).andExpect(status().isNotFound());
        lookup("QQQQQQQQQQQQQQQQ", "5678").andExpect(status().isTooManyRequests());
    }

    @Test
    void auditStoresNoPhoneCodeOrRawAddress() throws Exception {
        publish();
        lookup(code, "0000").andExpect(status().isNotFound());
        lookup(code, "5678").andExpect(status().isOk());
        // The audit rows are written in their own committed transaction, so read them from a fresh one.
        var fresh = new org.springframework.transaction.support.TransactionTemplate(txManager);
        fresh.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        List<String> rows = fresh.execute(status -> jdbc.query(
                "select concat_ws('|', ifnull(release_id,''), action, outcome, client_hash) from public_lookup_audit",
                (rs, n) -> rs.getString(1)));
        assertThat(rows).isNotEmpty();
        assertThat(String.join("\n", rows)).doesNotContain("0912345678", code, clientAddress);
    }

    @Test
    void staleStaffCookieDoesNotBreakPublicLookupAndInternalApiStaysClosed() throws Exception {
        publish();
        mvc.perform(post("/api/v1/public/results/lookup").with(from()).contentType("application/json").content(body(code, "5678"))
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "stale.invalid.token")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/result-releases/" + releaseId)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/public/results/anything")).andExpect(status().isUnauthorized());
    }
}
