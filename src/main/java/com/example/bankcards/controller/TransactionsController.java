package com.example.bankcards.controller;

import com.example.bankcards.dto.transfer.CreateTransferRequest;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.interfaces.TransactionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionsController {

    private final TransactionsService transactionsService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> createTransaction(
            @RequestBody CreateTransferRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal){

        transactionsService.createTransaction(request, userPrincipal.getId());
        return ResponseEntity.ok().build();
    }
}
