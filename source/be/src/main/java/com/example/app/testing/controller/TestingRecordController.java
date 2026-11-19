package com.example.app.testing.controller;

import com.example.app.testing.dto.ReviewTestingRecordRequest;
import com.example.app.testing.dto.SaveTestingRecordRequest;
import com.example.app.testing.dto.TestingRecordResponse;
import com.example.app.testing.service.TestingRecordService;
import com.example.app.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import com.example.app.common.dto.PagedResponse;
import com.example.app.testing.entity.TestRecordStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/testing-records")
public class TestingRecordController {
    public record FileResultRequest(@jakarta.validation.constraints.NotNull Long transferItemId) {}
    private final TestingRecordService testingRecordService;
    private final UserService userService;

    public TestingRecordController(TestingRecordService testingRecordService, UserService userService) {
        this.testingRecordService = testingRecordService;
        this.userService = userService;
    }

    @GetMapping
    public PagedResponse<TestingRecordResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) TestRecordStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return testingRecordService.search(query, status, from, to, page, size, sort);
    }

    /** Accepted samples that still need a testing record. */
    @GetMapping("/candidates")
    public List<TestingRecordService.Candidate> candidates() { return testingRecordService.candidates(); }

    @GetMapping("/{id}")
    public TestingRecordResponse get(@PathVariable Long id) { return testingRecordService.findById(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestingRecordResponse create(@Valid @RequestBody SaveTestingRecordRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        return testingRecordService.create(request, userService.getByEmail(jwt.getSubject()));
    }

    @PostMapping("/from-result-file")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    public TestingRecordResponse createFromResultFile(@Valid @RequestBody FileResultRequest request,
                                                       @AuthenticationPrincipal Jwt jwt) {
        return testingRecordService.createFromResultFile(request.transferItemId(),
                userService.getByEmail(jwt.getSubject()));
    }

    @PutMapping("/{id}")
    public TestingRecordResponse update(@PathVariable Long id,
                                        @Valid @RequestBody SaveTestingRecordRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        return testingRecordService.update(id, request, userService.getByEmail(jwt.getSubject()));
    }

    @PostMapping("/{id}/submit")
    public TestingRecordResponse submit(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return testingRecordService.submit(id, userService.getByEmail(jwt.getSubject()));
    }

    @PostMapping("/{id}/review")
    public TestingRecordResponse review(@PathVariable Long id,
                                        @Valid @RequestBody ReviewTestingRecordRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        return testingRecordService.review(id, request, userService.getByEmail(jwt.getSubject()));
    }
}
