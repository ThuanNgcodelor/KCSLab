package com.example.app.publicapi;

import com.example.app.results.service.ResultReleaseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Public, unauthenticated API. It is separate from the internal API and exposes no PII. POST is used for both
 * operations so the lookup code and the phone digits never appear in URLs, logs or the Referer header.
 */
@RestController
@RequestMapping("/api/v1/public/results")
public class PublicResultController {
    public record LookupRequest(@NotBlank @Size(max = 40) String code,
                                @NotBlank @Pattern(regexp = "\\d{4}", message = "Nhập đúng 4 số cuối điện thoại") String phoneLast4) {}
    public record DownloadRequest(@NotBlank @Size(max = 40) String code,
                                  @NotBlank @Pattern(regexp = "\\d{4}", message = "Nhập đúng 4 số cuối điện thoại") String phoneLast4,
                                  @NotNull Long fileId) {}

    private final PublicResultService service;
    private final String clientIpHeader;

    public PublicResultController(PublicResultService service,
                                  @Value("${app.public.client-ip-header:}") String clientIpHeader) {
        this.service = service; this.clientIpHeader = clientIpHeader;
    }

    @PostMapping("/lookup")
    public ResponseEntity<PublicResultService.PublicResult> lookup(@Valid @RequestBody LookupRequest request,
                                                                   HttpServletRequest http) {
        return noStore(ResponseEntity.ok()).body(service.lookup(request.code(), request.phoneLast4(), client(http)));
    }

    @PostMapping("/download")
    public ResponseEntity<Resource> download(@Valid @RequestBody DownloadRequest request, HttpServletRequest http) {
        var stored = service.download(request.code(), request.phoneLast4(), request.fileId(), client(http)).file();
        return noStore(ResponseEntity.ok())
                .contentType(MediaType.parseMediaType(stored.contentType()))
                .contentLength(stored.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(stored.originalName(), StandardCharsets.UTF_8).build().toString())
                .body(stored.resource());
    }

    @ExceptionHandler(PublicResultService.TooManyAttempts.class)
    ResponseEntity<ProblemDetail> tooMany(PublicResultService.TooManyAttempts exception) {
        return noStore(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS))
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()))
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage()));
    }

    private static <T extends ResponseEntity.HeadersBuilder<T>> T noStore(T builder) {
        return builder.cacheControl(CacheControl.noStore())
                .header("X-Robots-Tag", "noindex, nofollow, noarchive")
                .header("Referrer-Policy", "no-referrer")
                .header("X-Content-Type-Options", "nosniff");
    }

    private String client(HttpServletRequest request) {
        if (!clientIpHeader.isBlank()) {
            String forwarded = request.getHeader(clientIpHeader);
            // Use the entry appended by our own proxy (the last one): earlier entries are supplied by the client and spoofable.
            if (forwarded != null && !forwarded.isBlank()) {
                String[] hops = forwarded.split(",");
                String last = hops[hops.length - 1].trim();
                if (!last.isEmpty()) return last;
            }
        }
        return request.getRemoteAddr();
    }
}
