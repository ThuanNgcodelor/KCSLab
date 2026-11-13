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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public List<TransferResponse> list() { return transferService.findAll(); }

    @GetMapping("/{id}")
    public TransferResponse get(@PathVariable Long id) { return transferService.findById(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse create(@Valid @RequestBody CreateTransferRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return transferService.create(request, userService.getByEmail(jwt.getSubject()));
    }

    @PostMapping("/{id}/decision")
    public TransferResponse decide(@PathVariable Long id,
                                   @Valid @RequestBody TransferDecisionRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return transferService.decide(id, request, userService.getByEmail(jwt.getSubject()));
    }
}
