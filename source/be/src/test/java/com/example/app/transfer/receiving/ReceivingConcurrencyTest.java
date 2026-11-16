package com.example.app.transfer.receiving;

import com.example.app.intake.entity.AcceptanceDecision;
import com.example.app.intake.entity.Customer;
import com.example.app.intake.entity.Receipt;
import com.example.app.intake.entity.Sample;
import com.example.app.intake.repository.CustomerRepository;
import com.example.app.intake.repository.ReceiptRepository;
import com.example.app.transfer.dto.CreateTransferItemRequest;
import com.example.app.transfer.dto.CreateTransferRequest;
import com.example.app.transfer.dto.TransferDecisionRequest;
import com.example.app.transfer.entity.TransferStatus;
import com.example.app.transfer.service.TransferService;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Commits independent transactions, so it must only run against a disposable test database. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "KCS_ISOLATED_DB", matches = "true")
class ReceivingConcurrencyTest {
    @Autowired private TransactionTemplate transactions;
    @Autowired private UserRepository users;
    @Autowired private CustomerRepository customers;
    @Autowired private ReceiptRepository receipts;
    @Autowired private TransferService transfers;
    @Autowired private ReceivingService receiving;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void onlyOneConcurrentDecisionSucceedsAndOneNotificationIsCreated() throws Exception {
        String key = UUID.randomUUID().toString().substring(0, 8);
        var fixture = transactions.execute(status -> {
            var sender = users.save(new User("sender-" + key + "@example.test", "Sender", "unused", Role.RECEPTIONIST));
            var receiver = users.save(new User("receiver-" + key + "@example.test", "Receiver", "unused", Role.TECHNICIAN));
            var customer = customers.save(new Customer("Test", "0900000000", null, null));
            var receipt = new Receipt("TN-" + key, customer, sender, "Test", Instant.now(), "Test", "1",
                    "Túi", null, true, "Nguyên vẹn", null, null, null, "Thường", "Cd",
                    LocalDate.now().plusDays(1), null, AcceptanceDecision.ACCEPTED, null);
            receipt.addSample(new Sample("SM-" + key, "CT001", "Sầu riêng", LocalDate.now(), 1,
                    "Test", "1", "Túi", "Nguyên vẹn", "Cd"));
            receipt = receipts.saveAndFlush(receipt);
            var request = new CreateTransferRequest(receipt.getId(), "KCS", LocalDate.now(),
                    LocalDate.now().plusDays(1), false, null, null,
                    List.of(new CreateTransferItemRequest(receipt.getSamples().getFirst().getId(), "1", "1", "Tốt", "Cd", "Test")));
            var transfer = transfers.create(request, sender);
            transfers.submit(transfer.id());
            return new Fixture(sender.getId(), receiver.getId(), customer.getId(), receipt.getId(), transfer.id(), receiver);
        });
        assertThat(fixture).isNotNull();
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            var decision = new TransferDecisionRequest(TransferStatus.ACCEPTED, "Đủ mẫu", null, false, null);
            var task = (java.util.concurrent.Callable<Integer>) () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout waiting for test start");
                try { receiving.decide(fixture.transferId(), decision, fixture.receiver()); return 200; }
                catch (ResponseStatusException ex) { return ex.getStatusCode().value(); }
            };
            var one = executor.submit(task);
            var two = executor.submit(task);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
            assertThat(jdbc.queryForObject("select count(*) from staff_notifications where entity_type='TRANSFER' "
                    + "and entity_id=? and type='TRANSFER_ACCEPTED'", Long.class, fixture.transferId())).isEqualTo(1);
        } finally {
            transactions.executeWithoutResult(status -> {
                jdbc.update("delete from staff_notifications where entity_type='TRANSFER' and entity_id=?", fixture.transferId());
                jdbc.update("delete from sample_transfer_items where transfer_id=?", fixture.transferId());
                jdbc.update("delete from sample_transfers where id=?", fixture.transferId());
                jdbc.update("delete from samples where receipt_id=?", fixture.receiptId());
                jdbc.update("delete from receipts where id=?", fixture.receiptId());
                jdbc.update("delete from customers where id=?", fixture.customerId());
                jdbc.update("delete from users where id in (?, ?)", fixture.senderId(), fixture.receiverId());
            });
        }
    }

    private record Fixture(Long senderId, Long receiverId, Long customerId, Long receiptId, Long transferId, User receiver) {}
}
