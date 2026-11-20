package com.example.app.results.service;

import com.example.app.results.dto.ResultReleaseDetailResponse;
import com.example.app.testing.service.TestingRecordService;
import com.example.app.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Result-entry flow of /admin/testing/new: the analyst types only unit, result and note for each parameter; the sample
 * block of the KQKN report is filled from Phiếu 3/4. The testing record, the dossier and its rows are created together so
 * a failure never leaves a record without a dossier. Admin then approves (publishes) the dossier as before.
 */
@Service
public class ResultEntryService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final TestingRecordService records;
    private final ResultReleaseDraftService drafts;
    private final ResultReleaseService workflow;

    public ResultEntryService(TestingRecordService records, ResultReleaseDraftService drafts, ResultReleaseService workflow) {
        this.records = records; this.drafts = drafts; this.workflow = workflow;
    }

    @Transactional
    public ResultReleaseDetailResponse create(Long transferItemId, LocalDate testingDate,
                                              List<ResultReleaseService.ReportRowInput> rows, boolean submit, User actor) {
        Instant start = testingDate == null ? null : testingDate.atStartOfDay(BUSINESS_ZONE).toInstant();
        var record = records.createFromResultEntry(transferItemId, start, actor);
        Long releaseId = drafts.create(record.getId(), actor).id();
        ResultReleaseDetailResponse detail = workflow.updateReport(releaseId, rows, null, null, actor);
        return submit ? workflow.submit(releaseId, actor) : detail;
    }
}
