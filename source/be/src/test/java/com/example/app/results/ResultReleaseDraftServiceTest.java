package com.example.app.results;

import com.example.app.results.repository.ResultReleaseRepository;
import com.example.app.results.service.ResultReleaseDraftService;
import com.example.app.testing.entity.TestRecordStatus;
import com.example.app.testing.entity.TestingRecord;
import com.example.app.testing.repository.TestingRecordRepository;
import com.example.app.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResultReleaseDraftServiceTest {
    private final ResultReleaseRepository releases = mock(ResultReleaseRepository.class);
    private final TestingRecordRepository records = mock(TestingRecordRepository.class);
    private final ResultReleaseDraftService service = new ResultReleaseDraftService(releases, records,
            mock(com.example.app.results.repository.ResultReleaseEventRepository.class));
    private final User creator = mock(User.class);

    @Test void onlyReviewedRecordCanCreateDraft() {
        var record = mock(TestingRecord.class);
        when(records.findById(1L)).thenReturn(Optional.of(record));
        when(record.getStatus()).thenReturn(TestRecordStatus.DRAFT);
        assertThatThrownBy(() -> service.create(1L, creator)).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode().value()).isEqualTo(409));
        verify(releases, never()).saveAndFlush(any());
    }

    @Test void reviewedRecordCreatesDraftAndDuplicateIsRejected() {
        var record = mock(TestingRecord.class);
        when(records.findById(1L)).thenReturn(Optional.of(record));
        when(record.getStatus()).thenReturn(TestRecordStatus.REVIEWED);
        when(record.getId()).thenReturn(1L);
        when(releases.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.create(1L, creator).status()).isEqualTo("DRAFT");
        when(releases.existsByTestingRecord_IdAndVersion(1L, 1)).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L, creator)).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode().value()).isEqualTo(409));
    }
}
