package com.example.app.transfer.controller;

import com.example.app.transfer.dto.CreateTransferRequest;
import com.example.app.transfer.dto.TransferDecisionRequest;
import com.example.app.transfer.dto.TransferResponse;
import com.example.app.transfer.service.TransferService;
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

import com.example.app.common.dto.PagedResponse;
import com.example.app.transfer.entity.TransferStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {
    private final TransferService transferService;
    private final UserService userService;

    public TransferController(TransferService transferService, UserService userService) {
        this.transferService = transferService;
        this.userService = userService;
    }

    @GetMapping
    public PagedResponse<TransferResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal Jwt jwt) {
        return transferService.search(userService.getByEmail(jwt.getSubject()), query, status, from, to, page, size, sort);
    }

    @GetMapping("/{id}")
    public TransferResponse get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return transferService.findById(id, userService.getByEmail(jwt.getSubject()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse create(@Valid @RequestBody CreateTransferRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return transferService.create(request, userService.getByEmail(jwt.getSubject()));
    }

    @PutMapping("/{id}")
    public TransferResponse update(@PathVariable Long id,
                                   @Valid @RequestBody CreateTransferRequest request) {
        return transferService.update(id, request);
    }

    @PostMapping("/{id}/submit")
    public TransferResponse submit(@PathVariable Long id) {
        return transferService.submit(id);
    }

    @PostMapping("/{id}/decision")
    public TransferResponse decide(@PathVariable Long id,
                                   @Valid @RequestBody TransferDecisionRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return transferService.decide(id, request, userService.getByEmail(jwt.getSubject()));
    }
}
