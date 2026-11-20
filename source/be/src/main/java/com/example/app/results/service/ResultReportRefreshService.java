package com.example.app.results.service;

import com.example.app.results.repository.ResultReleaseRepository;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Rebuilds the PDF of every published KQKN, e.g. once after PUBLIC_LOOKUP_URL moves from the LAN IP to the real
 * domain, so the QR in the downloadable PDF points to the new address. Number and lookup code never change.
 * Deliberately not transactional: each report is rebuilt in its own transaction (LibreOffice takes a few seconds),
 * and one failure does not undo the others.
 */
@Service
public class ResultReportRefreshService {
    private final ResultReleaseRepository releases;
    private final ResultReleaseService workflow;
    private final String lookupUrl;

    public ResultReportRefreshService(ResultReleaseRepository releases, ResultReleaseService workflow,
                                      @Value("${app.public.lookup-url:}") String lookupUrl) {
        this.releases = releases; this.workflow = workflow; this.lookupUrl = lookupUrl == null ? "" : lookupUrl.trim();
    }

    public record Settings(String lookupUrl, int publishedCount) {}
    public record Result(String lookupUrl, int regenerated, int skipped, List<String> failures) {}

    public Settings settings() { return new Settings(lookupUrl, releases.findPublishedIds().size()); }

    public Result regenerateAll(User actor) {
        if (actor.getRole() != Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Admin được tạo lại phiếu PDF");
        int regenerated = 0, skipped = 0;
        List<String> failures = new ArrayList<>();
        for (Long id : releases.findPublishedIds()) {
            try {
                workflow.regenerateReport(id, actor);
                regenerated++;
            } catch (ResponseStatusException exception) {
                if (exception.getStatusCode().value() == HttpStatus.CONFLICT.value()) skipped++; // uploaded report file
                else failures.add("#" + id + ": " + exception.getReason());
            } catch (RuntimeException exception) {
                failures.add("#" + id + ": " + exception.getMessage());
            }
        }
        return new Result(lookupUrl, regenerated, skipped, failures);
    }
}
