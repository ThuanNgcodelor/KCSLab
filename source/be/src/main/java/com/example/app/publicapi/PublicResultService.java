package com.example.app.publicapi;

import com.example.app.results.entity.ResultRelease;
import com.example.app.results.entity.ResultReleaseFile;
import com.example.app.results.entity.ResultReleaseStatus;
import com.example.app.results.repository.ResultReleaseFileRepository;
import com.example.app.results.repository.ResultReleaseItemRepository;
import com.example.app.results.repository.ResultReleaseRepository;
import com.example.app.results.service.ResultReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Public result lookup. Every request, whether it comes from a typed code or a QR link, must present the lookup
 * code and the last four digits of the customer's phone. Only PUBLISHED dossiers are visible; wrong code, wrong
 * phone, draft, revoked and missing all produce the same neutral answer. State is re-checked on every download.
 */
@Service
public class PublicResultService {
    static final String NEUTRAL_NOT_FOUND = "Không tìm thấy kết quả phù hợp với thông tin đã nhập";
    private static final int MAX_CODE_FAILURES = 5;
    private static final int MAX_CLIENT_FAILURES = 15;
    private static final int MAX_CLIENT_REQUESTS = 60;

    private final ResultReleaseRepository releases;
    private final ResultReleaseFileRepository files;
    private final ResultReleaseItemRepository items;
    private final ResultReleaseService storage;
    private final PublicLookupAudit audit;
    private final String pepper;
    private final PublicLookupRateLimiter codeFailures;
    private final PublicLookupRateLimiter clientFailures;
    private final PublicLookupRateLimiter clientRequests;

    @Autowired
    public PublicResultService(ResultReleaseRepository releases, ResultReleaseFileRepository files,
                               ResultReleaseItemRepository items, ResultReleaseService storage, PublicLookupAudit audit,
                               @Value("${app.security.jwt-secret}") String pepper) {
        this(releases, files, items, storage, audit, pepper, Clock.systemUTC());
    }

    PublicResultService(ResultReleaseRepository releases, ResultReleaseFileRepository files,
                        ResultReleaseItemRepository items, ResultReleaseService storage, PublicLookupAudit audit, String pepper, Clock clock) {
        this.releases = releases; this.files = files; this.items = items; this.storage = storage; this.audit = audit; this.pepper = pepper;
        this.codeFailures = new PublicLookupRateLimiter(MAX_CODE_FAILURES, Duration.ofMinutes(15), clock);
        this.clientFailures = new PublicLookupRateLimiter(MAX_CLIENT_FAILURES, Duration.ofMinutes(15), clock);
        this.clientRequests = new PublicLookupRateLimiter(MAX_CLIENT_REQUESTS, Duration.ofMinutes(10), clock);
    }

    public record PublicFile(Long id, String name, String contentType, long size) {}
    /** What a customer may see: the report's result rows and the PDF. No customer name, address or phone. */
    public record PublicItem(String parameter, String unit, String method, String result, String note) {}
    public record PublicResult(String resultNumber, int version, String sampleCode, String sampleName,
                               Instant publishedAt, List<PublicItem> items, List<PublicFile> files) {}
    public record Download(ResultReleaseService.StoredFile file) {}

    /** Thrown for throttling; mapped to HTTP 429 with Retry-After by the controller advice. */
    public static class TooManyAttempts extends RuntimeException {
        private final long retryAfterSeconds;
        TooManyAttempts(long retryAfterSeconds) { super("Quá nhiều lần thử, vui lòng thử lại sau"); this.retryAfterSeconds = retryAfterSeconds; }
        public long retryAfterSeconds() { return retryAfterSeconds; }
    }

    @Transactional(readOnly = true)
    public PublicResult lookup(String rawCode, String phoneLast4, String clientAddress) {
        ResultRelease release = verify("LOOKUP", rawCode, phoneLast4, clientAddress);
        List<PublicFile> publicFiles = files.findByRelease_IdAndRemovedAtIsNullOrderByIdAsc(release.getId()).stream()
                .filter(ResultReleaseFile::isPublicDownload)
                .map(f -> new PublicFile(f.getId(), f.getOriginalName(), f.getContentType(), f.getSize())).toList();
        List<PublicItem> rows = items.findByRelease_IdOrderByRowOrderAsc(release.getId()).stream()
                .map(row -> new PublicItem(row.getParameterName(), row.getUnit(), row.getMethod(), row.getResultValue(),
                        row.getNote())).toList();
        var item = release.getTestingRecord().getTransferItem();
        return new PublicResult(release.getResultNumber(), release.getVersion(), item.getSampleCode(),
                item.getTransfer().getReceipt().getSampleName(), release.getPublishedAt(), rows, publicFiles);
    }

    @Transactional(readOnly = true)
    public Download download(String rawCode, String phoneLast4, Long fileId, String clientAddress) {
        ResultRelease release = verify("DOWNLOAD", rawCode, phoneLast4, clientAddress);
        ResultReleaseFile file = files.findByIdAndRelease_Id(fileId, release.getId())
                .filter(candidate -> !candidate.isRemoved() && candidate.isPublicDownload())
                .orElse(null);
        if (file == null) {
            audit.record(release.getId(), "DOWNLOAD", "NOT_FOUND", clientHash(clientAddress));
            throw notFound();
        }
        audit.record(release.getId(), "DOWNLOAD", "OK", clientHash(clientAddress));
        return new Download(storage.open(file));
    }

    private ResultRelease verify(String action, String rawCode, String phoneLast4, String clientAddress) {
        String client = clientHash(clientAddress);
        String code = normalizeCode(rawCode);
        String codeKey = sha256(pepper + "|code|" + code);

        if (clientRequests.isBlocked(client) || clientFailures.isBlocked(client) || codeFailures.isBlocked(codeKey)) {
            audit.record(null, action, "RATE_LIMITED", client);
            long retry = Math.max(clientRequests.retryAfterSeconds(client),
                    Math.max(clientFailures.retryAfterSeconds(client), codeFailures.retryAfterSeconds(codeKey)));
            throw new TooManyAttempts(retry);
        }
        clientRequests.record(client);

        ResultRelease release = code.length() == 16 ? releases.findByLookupCode(code).orElse(null) : null;
        String phone = release == null ? "" : phoneOf(release);
        boolean phoneOk = phoneMatches(phone, phoneLast4);
        boolean visible = release != null && release.getStatus() == ResultReleaseStatus.PUBLISHED && phoneOk;
        if (!visible) {
            codeFailures.record(codeKey);
            clientFailures.record(client);
            audit.record(release == null ? null : release.getId(), action, "NOT_FOUND", client);
            throw notFound();
        }
        codeFailures.clear(codeKey);
        if ("LOOKUP".equals(action)) audit.record(release.getId(), action, "OK", client);
        return release;
    }

    private String phoneOf(ResultRelease release) {
        return release.getTestingRecord().getTransferItem().getTransfer().getReceipt().getCustomer().getPhone();
    }

    /** Constant-time comparison of the last four digits; never logs or returns the phone number. */
    static boolean phoneMatches(String storedPhone, String last4) {
        String digits = storedPhone == null ? "" : storedPhone.replaceAll("\\D", "");
        String expected = digits.length() >= 4 ? digits.substring(digits.length() - 4) : "????";
        String given = last4 != null && last4.matches("\\d{4}") ? last4 : "!!!!";
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8))
                && digits.length() >= 4;
    }

    static String normalizeCode(String raw) {
        return raw == null ? "" : raw.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, NEUTRAL_NOT_FOUND);
    }

    private String clientHash(String clientAddress) { return sha256(pepper + "|client|" + clientAddress); }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
