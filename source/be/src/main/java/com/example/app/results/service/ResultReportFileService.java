package com.example.app.results.service;

import com.example.app.results.dto.ResultReleaseDetailResponse;
import com.example.app.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Accepts the KQKN report as a single file (DOCX or PDF). Deliberately not transactional: the slow LibreOffice
 * conversion must not hold a database transaction or row lock; only the final save runs in one.
 */
@Service
public class ResultReportFileService {
    private final ResultReleaseService workflow;
    private final OfficeConverter converter;
    private final ReportFileStamper stamper;

    public ResultReportFileService(ResultReleaseService workflow, OfficeConverter converter, ReportFileStamper stamper) {
        this.workflow = workflow; this.converter = converter; this.stamper = stamper;
    }

    public ResultReleaseDetailResponse upload(Long id, MultipartFile upload, User actor) {
        workflow.requireReportEditable(id, actor);
        var file = ResultFilePolicy.validate(upload);
        if (file.extension().equals("xlsx")) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Phiếu kết quả chỉ nhận file DOCX hoặc PDF");
        }
        byte[] pdf = file.isPdf() ? file.content() : converter.docxToPdf(file.content());
        ResultFilePolicy.checkPdf(pdf);
        var inspection = stamper.inspect(pdf);
        // Dry run: proves the stamping step works on this file now, instead of failing later at publication.
        stamper.stamp(pdf, null, true);
        return workflow.attachReport(id, new ResultReleaseService.PreparedReport(file.originalName(), file.extension(),
                file.contentType(), file.isPdf() ? null : file.content(), pdf, inspection.documentNumber(),
                inspection.slot().placeholderFound()), actor);
    }
}
