package com.example.app.sampling.service;

import com.example.app.sampling.dto.TestRequestDtos.*;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class TestRequestNumberingTest {
    @Autowired private TestRequestNumberSequence numbers;
    @Autowired private TestRequestService service;
    @Autowired private UserRepository users;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager tx;
    @Autowired private WebApplicationContext context;
    private User actor;

    @BeforeEach
    void setup() {
        actor = users.save(new User("req-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test", "Req", "unused", Role.RECEPTIONIST));
    }

    @Test
    @Transactional
    void allocatesSequentiallyWithFourDigitsAndPerYearCounter() {
        assertThat(numbers.allocate("98")).isEqualTo("CFC98.0001");
        assertThat(numbers.allocate("98")).isEqualTo("CFC98.0002");
        assertThat(numbers.allocate("99")).isEqualTo("CFC99.0001");
    }

    @Test
    @Transactional
    void handTypedNumberRaisesTheCounterAndGrowsADigitAfter9999() {
        numbers.register("CFC97.0010");
        assertThat(numbers.allocate("97")).isEqualTo("CFC97.0011");
        numbers.register("CFC96.9999");
        assertThat(numbers.allocate("96")).isEqualTo("CFC96.10000");
        numbers.register("CFC96.0005"); // lower numbers never move the counter back
        assertThat(numbers.allocate("96")).isEqualTo("CFC96.10001");
    }

    @Test
    void yearCodeFollowsVietnamTimeAndRollsOver() {
        Clock newYear = Clock.fixed(Instant.parse("2026-12-31T17:30:00Z"), ZoneOffset.UTC); // already 1 Jan 2027 in Vietnam
        Clock inYear = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC);
        assertThat(new TestRequestNumberSequence(jdbc, newYear).currentYearCode()).isEqualTo("27");
        assertThat(new TestRequestNumberSequence(jdbc, inYear).currentYearCode()).isEqualTo("26");
        assertThat(TestRequestNumberSequence.format("27", 1)).isEqualTo("CFC27.0001");
    }

    @Test
    @Transactional
    void blankNumberIsAllocatedAutomaticallyAndTypedNumbersAreValidated() {
        Response auto = service.create(request(" "), actor);
        assertThat(auto.requestNumber()).matches("^CFC\\d{2}\\.\\d{4,}$");

        Response typed = service.create(request("cfc95.0007"), actor);
        assertThat(typed.requestNumber()).isEqualTo("CFC95.0007");

        assertThatThrownBy(() -> service.create(request("cfc95.0007"), actor))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode().value()).isEqualTo(409);
                    assertThat(e.getReason()).contains("CFC95.0007").contains("CFC95.0008");
                });
        assertThatThrownBy(() -> service.create(request("PHIEU-1"), actor))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        assertThatThrownBy(() -> service.create(request("CFC26.12"), actor))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));

        Response next = service.create(request(""), actor);
        assertThat(next.requestNumber()).isNotEqualTo(auto.requestNumber());
    }

    @Test
    @Transactional
    void requestNumberCannotChangeButMayBeOmittedOnUpdate() {
        Response created = service.create(request("CFC94.0001"), actor);
        assertThat(service.update(created.id(), request(""), actor).requestNumber()).isEqualTo("CFC94.0001");
        assertThatThrownBy(() -> service.update(created.id(), request("CFC94.0002"), actor))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @Transactional
    void planAndFieldSamplingShareTheRequestNumberWhateverTheClientSends() {
        Response created = service.create(request("CFC93.0001"), actor);
        Response withPlan = service.savePlan(created.id(), plan("OTHER-1"), actor);
        Response withField = service.saveField(created.id(), field(null), actor);
        assertThat(withPlan.samplingPlan().formNumber()).isEqualTo("CFC93.0001");
        assertThat(withField.fieldSampling().formNumber()).isEqualTo("CFC93.0001");
        // a second save keeps the stored number and does not collide with itself
        assertThat(service.savePlan(created.id(), plan(null), actor).samplingPlan().formNumber()).isEqualTo("CFC93.0001");
    }

    @Test
    @Transactional
    void checklistsOfPlanAndFieldSamplingRoundTrip() {
        Response created = service.create(request("CFC90.0001"), actor);
        SavePlan plan = new SavePlan(null, LocalDate.of(2026, 11, 19), "Anh Hưng", "QLCLSP", null, null, null, null, null, null, null, null, null, null, null,
                "0939740009", "WHOLE,FLESH", "1_5", "WHOLE_AMBIENT,FRESH_COLD");
        PlanResponse p = service.savePlan(created.id(), plan, actor).samplingPlan();
        assertThat(p.representativePhone()).isEqualTo("0939740009");
        assertThat(p.sampleForms()).isEqualTo("WHOLE,FLESH");
        assertThat(p.lotSizeBand()).isEqualTo("1_5");
        assertThat(p.transportModes()).isEqualTo("WHOLE_AMBIENT,FRESH_COLD");

        SaveField field = new SaveField(null, LocalDate.of(2026, 11, 19), "Anh Hưng", "Người lấy mẫu", null, null, null, null, null, null, null, null, null,
                "Nguyên vẹn", "Lạnh", "S-001", true, "Ngẫu nhiên", "5 quả", null, "0939740009", "4",
                List.of(new FieldItemRequest("Mẫu 1", "Ngẫu nhiên", "5 quả", "VANG_O,CD", null), new FieldItemRequest("Mẫu 2", null, null, "", "Pb khác")));
        FieldResponse f = service.saveField(created.id(), field, actor).fieldSampling();
        assertThat(f.storageTemperature()).isEqualTo("4");
        assertThat(f.items()).hasSize(2);
        assertThat(f.items().getFirst().parameters()).isEqualTo("VANG_O,CD");
        assertThat(f.items().get(1).parametersOther()).isEqualTo("Pb khác");
        // items omitted -> rows kept; items sent -> replaced
        assertThat(service.saveField(created.id(), field(null), actor).fieldSampling().items()).hasSize(2);
        assertThat(service.saveField(created.id(), new SaveField(null, LocalDate.of(2026, 11, 19), null, "X", null, null, null, null, null, null, null, null, null,
                null, null, null, true, null, null, null, null, null, List.of()), actor).fieldSampling().items()).isEmpty();
    }

    @Test
    @Transactional
    void listIsPagedSearchedFilteredAndSortedInTheDatabase() {
        for (int i = 1; i <= 3; i++) service.create(request("CFC89.000" + i), actor);
        Response done = service.create(new SaveRequest("CFC89.0004", LocalDate.of(2026, 11, 20), "Công ty Xuất Khẩu ZZ", null, null, null, "0911111111", null,
                "Sầu riêng", "LOT-ZZ", null, null, null, null, "CUSTOMER", null, null, null, null, null, true,
                List.of(new ItemRequest("", "Cadimi (Cd)", "Ruột", null))), actor);

        var page = service.search("CFC89", null, null, null, null, 0, 2, "requestNumber,asc");
        assertThat(page.totalElements()).isEqualTo(4);
        assertThat(page.content()).hasSize(2);
        assertThat(page.content().getFirst().requestNumber()).isEqualTo("CFC89.0001");
        assertThat(page.totalPages()).isEqualTo(2);

        assertThat(service.search("xuất khẩu", null, null, null, null, 0, 20, null).content()).extracting(Response::id).containsExactly(done.id());
        assertThat(service.search("lot-zz", null, null, null, null, 0, 20, null).totalElements()).isEqualTo(1);
        assertThat(service.search("CFC89", "COMPLETED", null, null, null, 0, 20, null).totalElements()).isEqualTo(1);
        assertThat(service.search("CFC89", null, "CUSTOMER", null, null, 0, 20, null).totalElements()).isEqualTo(1);
        assertThat(service.search("CFC89", null, null, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 20), 0, 20, null).totalElements()).isEqualTo(1);
        assertThat(service.search("100%", null, null, null, null, 0, 20, null).totalElements()).isZero();
        assertThat(service.search("CFC89", null, null, null, null, 0, 20, "customerName,desc").content().getFirst().customerName()).isEqualTo("Khách A");

        assertThatThrownBy(() -> service.search(null, null, null, null, null, 0, 20, "password,asc"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        assertThatThrownBy(() -> service.search(null, "BOGUS", null, null, null, 0, 20, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        assertThatThrownBy(() -> service.search(null, null, null, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1), 0, 20, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @Transactional
    void blankSampleLabelGetsADefaultName() {
        Response created = service.create(request("CFC92.0001"), actor);
        assertThat(created.items().getFirst().sampleNameOrId()).isEqualTo("Mẫu 1");
    }

    @Test
    void nextNumberEndpointIsForReceptionistAndAdminOnly() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(get("/api/v1/test-requests/next-number")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN")))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/test-requests/next-number")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST")))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/test-requests/next-number")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))).andExpect(status().isOk());
    }

    /** Commits independent transactions, so it only runs against a disposable test database. */
    @Test
    @EnabledIfEnvironmentVariable(named = "KCS_ISOLATED_DB", matches = "true")
    void concurrentAllocationsNeverShareANumber() throws Exception {
        int workers = 16;
        var pool = Executors.newFixedThreadPool(workers);
        var start = new CountDownLatch(1);
        TransactionTemplate template = new TransactionTemplate(tx);
        List<Future<String>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < workers; i++) {
                futures.add(pool.submit(() -> { start.await(); return template.execute(s -> numbers.allocate("91")); }));
            }
            start.countDown();
            List<String> all = new ArrayList<>();
            for (Future<String> f : futures) all.add(f.get(60, TimeUnit.SECONDS));
            assertThat(all).doesNotHaveDuplicates().hasSize(workers);
            assertThat(jdbc.queryForObject("select last_sequence from test_request_counters where year_code = '91'", Integer.class)).isEqualTo(workers);
        } finally {
            pool.shutdownNow();
            jdbc.update("delete from test_request_counters where year_code = '91'");
        }
    }

    private static SaveRequest request(String number) {
        return new SaveRequest(number, LocalDate.of(2026, 11, 19), "Khách A", null, null, null, "0900000000", null,
                "Sầu riêng", null, null, null, null, null, "COMPANY", null, null, null, null, null, false,
                List.of(new ItemRequest("", "Cadimi (Cd)", "Ruột", null)));
    }

    private static SavePlan plan(String number) {
        return new SavePlan(number, LocalDate.of(2026, 11, 19), null, "Người lấy mẫu", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static SaveField field(String number) {
        return new SaveField(number, LocalDate.of(2026, 11, 19), null, "Người lấy mẫu", null, null, null, null, null, null, null, null, null,
                null, null, null, true, null, null, null, null, null, null);
    }
}
