package com.example.app.admin.service;

import com.example.app.admin.dto.AdminDashboardResponse;
import com.example.app.intake.dto.IntakeDashboardResponse;
import com.example.app.intake.service.IntakeDashboardService;
import com.example.app.results.entity.ResultReleaseStatus;
import com.example.app.results.repository.ResultReleaseRepository;
import com.example.app.testing.entity.TestRecordStatus;
import com.example.app.testing.repository.TestingRecordRepository;
import com.example.app.transfer.entity.TransferStatus;
import com.example.app.transfer.repository.SampleTransferRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
public class AdminDashboardService {
    private final IntakeDashboardService intakeDashboardService;
    private final SampleTransferRepository transfers;
    private final TestingRecordRepository testingRecords;
    private final ResultReleaseRepository releases;

    public AdminDashboardService(IntakeDashboardService intakeDashboardService,
                                 SampleTransferRepository transfers,
                                 TestingRecordRepository testingRecords,
                                 ResultReleaseRepository releases) {
        this.intakeDashboardService = intakeDashboardService;
        this.transfers = transfers;
        this.testingRecords = testingRecords;
        this.releases = releases;
    }

    public AdminDashboardResponse summarize(LocalDate requestedFrom, LocalDate requestedTo) {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate from = requestedFrom == null ? today.minusDays(6) : requestedFrom;
        LocalDate to = requestedTo == null ? today : requestedTo;
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu không được sau ngày kết thúc");
        }

        IntakeDashboardResponse intake = intakeDashboardService.summarize(from, to, null);
        return new AdminDashboardResponse(from, to, intake.totalReceipts(), intake.totalSamples(),
                transfers.countByStatus(TransferStatus.PENDING),
                transfers.countByStatus(TransferStatus.ACCEPTED),
                transfers.countByStatus(TransferStatus.REJECTED),
                testingRecords.countByStatus(TestRecordStatus.DRAFT),
                testingRecords.countByStatus(TestRecordStatus.SUBMITTED),
                testingRecords.countByStatus(TestRecordStatus.REVIEWED),
                releases.countByStatus(ResultReleaseStatus.DRAFT),
                releases.countByStatus(ResultReleaseStatus.SUBMITTED),
                releases.countByStatus(ResultReleaseStatus.PUBLISHED),
                releases.countByStatus(ResultReleaseStatus.REVOKED));
    }
}
