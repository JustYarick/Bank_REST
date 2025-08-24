package com.example.bankcards.controller;

import com.example.bankcards.dto.requests.CardBlockRequest;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.interfaces.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/request")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @PostMapping("/card_block")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> blockCard(
            @RequestBody @Valid CardBlockRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        requestService.createCardBlockRequest(request, userPrincipal.getId());
        return ResponseEntity.ok().build();
    }
}
