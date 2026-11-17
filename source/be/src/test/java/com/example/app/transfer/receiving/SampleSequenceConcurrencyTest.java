package com.example.app.transfer.receiving;

import com.example.app.intake.dto.CreateReceiptRequest;
import com.example.app.intake.dto.ReceiptResponse;
import com.example.app.intake.dto.SampleRequest;
import com.example.app.intake.entity.AcceptanceDecision;
import com.example.app.intake.entity.SealStatus;
import com.example.app.intake.service.ReceiptService;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Commits independent transactions, so it only runs against a disposable test database. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "KCS_ISOLATED_DB", matches = "true")
class SampleSequenceConcurrencyTest {
    @Autowired private ReceiptService receipts;
    @Autowired private UserRepository users;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void concurrentReceiptsNeverShareADailySequenceAndTheVietnamDateDecidesTheCode() throws Exception {
        String key = UUID.randomUUID().toString().substring(0, 8);
        User receiver = users.save(new User("seq-" + key + "@example.test", "Seq", "unused", Role.RECEPTIONIST));
        // 17:30Z on 31 Dec is already 1 Jan in Vietnam (UTC+7); the date inside the code must follow Vietnam.
        Instant receivedAt = Instant.parse("2003-12-31T17:30:00Z");
        int workers = 12;
        var pool = Executors.newFixedThreadPool(workers);
        var start = new CountDownLatch(1);
        List<Future<ReceiptResponse>> futures = new ArrayList<>();
        List<Long> receiptIds = new ArrayList<>();
        try {
            for (int i = 0; i < workers; i++) {
                int n = i;
                futures.add(pool.submit(() -> {
                    start.await();
                    return receipts.create(request(key, n, receivedAt), receiver);
                }));
            }
            start.countDown();
            List<String> codes = new ArrayList<>();
            for (Future<ReceiptResponse> future : futures) {
                ReceiptResponse response = future.get(60, TimeUnit.SECONDS);
                receiptIds.add(response.id());
                codes.add(response.samples().getFirst().sampleCode());
            }

            assertThat(codes).doesNotHaveDuplicates().allMatch(code -> code.contains("-01012004-"));
            Integer max = jdbc.queryForObject("select max(daily_sequence) from samples where received_date = ?",
                    Integer.class, LocalDate.of(2004, 1, 1));
            assertThat(max).isGreaterThanOrEqualTo(workers);
        } finally {
            pool.shutdownNow();
            cleanUp(receiptIds, receiver);
        }
    }

    /** Leaves no committed rows behind, because other tests assume an otherwise empty database. */
    private void cleanUp(List<Long> receiptIds, User receiver) {
        for (Long id : receiptIds) {
            jdbc.update("delete from staff_notifications where entity_type = 'RECEIPT' and entity_id = ?", id);
            jdbc.update("delete from samples where receipt_id = ?", id);
        }
        List<Long> customerIds = receiptIds.isEmpty() ? List.of() : jdbc.queryForList(
                "select distinct customer_id from receipts where id in (" + "?,".repeat(receiptIds.size() - 1) + "?)",
                Long.class, receiptIds.toArray());
        for (Long id : receiptIds) jdbc.update("delete from receipts where id = ?", id);
        for (Long id : customerIds) jdbc.update("delete from customers where id = ?", id);
        jdbc.update("delete from sample_code_counters where counter_date = ?", LocalDate.of(2004, 1, 1));
        jdbc.update("delete from staff_notifications where recipient_user_id = ?", receiver.getId());
        jdbc.update("delete from users where id = ?", receiver.getId());
    }

    private CreateReceiptRequest request(String key, int n, Instant receivedAt) {
        return new CreateReceiptRequest("Khách " + key + n, "09" + String.format("%08d", Math.abs((key + n).hashCode()) % 100000000),
                "REQ-" + key + n, null, null, null, receivedAt.minusSeconds(60), null, null, null, null, null,
                "Người giao", receivedAt, "Sầu riêng", "1 kg", "Túi", null, SealStatus.INTACT, "Nguyên vẹn",
                null, null, null, "Thường", "Cadimi", LocalDate.of(2004, 1, 5), null,
                AcceptanceDecision.ACCEPTED, null,
                List.of(new SampleRequest("CT201", "SR", "Mẫu " + n, "1 kg", "Túi", "Nguyên vẹn", "Cadimi")));
    }
}
