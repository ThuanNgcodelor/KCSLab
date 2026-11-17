package com.example.app.results.service;

import com.example.app.results.dto.ResultReleaseDraftResponse;
import com.example.app.results.entity.ResultRelease;
import com.example.app.results.repository.ResultReleaseRepository;
import com.example.app.testing.entity.TestRecordStatus;
import com.example.app.testing.repository.TestingRecordRepository;
import com.example.app.user.entity.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ResultReleaseDraftService {
    private final ResultReleaseRepository releases;
    private final TestingRecordRepository records;
    public ResultReleaseDraftService(ResultReleaseRepository releases, TestingRecordRepository records) {
        this.releases = releases; this.records = records;
    }

    @Transactional
    public ResultReleaseDraftResponse create(Long testingRecordId, User creator) {
        var record = records.findById(testingRecordId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biên bản thử nghiệm"));
        if (record.getStatus() != TestRecordStatus.REVIEWED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Biên bản thử nghiệm phải được kiểm tra trước khi lập hồ sơ kết quả");
        if (releases.existsByTestingRecord_IdAndVersion(testingRecordId, 1))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hồ sơ kết quả đã tồn tại");
        try { return ResultReleaseDraftResponse.from(releases.saveAndFlush(new ResultRelease(record, creator))); }
        catch (DataIntegrityViolationException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể tạo hồ sơ kết quả trùng", ex); }
    }

    @Transactional(readOnly = true)
    public ResultReleaseDraftResponse find(Long id) {
        return ResultReleaseDraftResponse.from(releases.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ kết quả")));
    }
}
