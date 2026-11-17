package com.example.app.intake.controller;

import com.example.app.intake.dto.SampleCodeEventResponse;
import com.example.app.intake.dto.SampleCodingResponse;
import com.example.app.intake.service.SampleCodingService;
import com.example.app.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Access is restricted to ADMIN and RECEPTIONIST in SecurityConfig. */
@RestController
@RequestMapping("/api/v1/samples/{sampleId}/coding")
public class SampleCodingController {
    public record ReasonRequest(@NotBlank @Size(max = 500) String reason) {}
    public record CorrectionRequest(@Size(max = 20) String sourceCode, @Size(max = 80) String productType,
                                    @NotBlank @Size(max = 500) String reason) {}

    private final SampleCodingService coding;
    private final UserService users;

    public SampleCodingController(SampleCodingService coding, UserService users) {
        this.coding = coding; this.users = users;
    }

    @GetMapping
    public SampleCodingResponse get(@PathVariable Long sampleId) { return coding.find(sampleId); }

    @GetMapping("/events")
    public List<SampleCodeEventResponse> events(@PathVariable Long sampleId) { return coding.history(sampleId); }

    @PostMapping("/submit")
    public SampleCodingResponse submit(@PathVariable Long sampleId, @AuthenticationPrincipal Jwt jwt) {
        return coding.submit(sampleId, users.getByEmail(jwt.getSubject()));
    }

    @PostMapping("/verify")
    public SampleCodingResponse verify(@PathVariable Long sampleId, @AuthenticationPrincipal Jwt jwt) {
        return coding.verify(sampleId, users.getByEmail(jwt.getSubject()));
    }

    @PostMapping("/reject")
    public SampleCodingResponse reject(@PathVariable Long sampleId, @Valid @RequestBody ReasonRequest request,
                                       @AuthenticationPrincipal Jwt jwt) {
        return coding.reject(sampleId, request.reason(), users.getByEmail(jwt.getSubject()));
    }

    @PostMapping("/correct")
    public SampleCodingResponse correct(@PathVariable Long sampleId, @Valid @RequestBody CorrectionRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        return coding.correct(sampleId, request.sourceCode(), request.productType(), request.reason(),
                users.getByEmail(jwt.getSubject()));
    }
}
