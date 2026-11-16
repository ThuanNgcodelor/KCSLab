package com.example.app.transfer.receiving;

import com.example.app.intake.service.ReceiptService;
import com.example.app.transfer.dto.TransferResponse;
import com.example.app.transfer.entity.TransferStatus;
import com.example.app.transfer.service.TransferService;
import com.example.app.transfer.repository.SampleTransferRepository;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ReceivingServiceTest {
    private final TransferService transfers = mock(TransferService.class);
    private final ReceiptService receipts = mock(ReceiptService.class);
    private final ReceivingService service = new ReceivingService(transfers, receipts,
            mock(SampleTransferRepository.class), mock(JdbcTemplate.class));
    private final User viewer = new User("kcs@example.test", "KCS", "unused", Role.TECHNICIAN);

    @Test
    void projectionDoesNotContainCustomerName() {
        when(transfers.findById(10L, viewer)).thenReturn(transfer(Instant.now()));
        var result = service.detail(10L, viewer);
        assertThat(result.transferCode()).isEqualTo("CM-001");
        assertThat(result.getClass().getRecordComponents())
                .extracting(component -> component.getName())
                .doesNotContain("customerName", "customerPhone", "customerEmail", "customerAddress");
    }

    @Test
    void contentIsLoadedOnlyThroughReceiptBelongingToVisibleTransfer() {
        when(transfers.findById(10L, viewer)).thenReturn(transfer(Instant.now()));
        when(receipts.loadAttachment(20L, 99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.content(10L, 99L, viewer))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(404));
        verify(receipts).loadAttachment(20L, 99L);
    }

    @Test
    void draftCannotExposeAttachmentsEvenToAdministrator() {
        User admin = new User("admin@example.test", "Admin", "unused", Role.ADMIN);
        when(transfers.findById(10L, admin)).thenReturn(transfer(null));
        assertThatThrownBy(() -> service.evidence(10L, admin))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(404));
        verifyNoInteractions(receipts);
    }

    private TransferResponse transfer(Instant submittedAt) {
        return new TransferResponse(10L, "CM-001", 20L, "TN-001", "Tên khách hàng riêng tư",
                "KCS", "Người giao nội bộ", null, LocalDate.now(), LocalDate.now(),
                false, null, null, submittedAt == null ? TransferStatus.DRAFT : TransferStatus.PENDING,
                null, null, false, null, submittedAt, null, Instant.now(), List.of());
    }
}
