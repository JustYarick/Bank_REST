package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.CreateCardResponse;
import com.example.bankcards.dto.core.PagedResponse;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.interfaces.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/card")
@Tag(name = "Cards", description = "Управление банковскими картами")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @GetMapping("/{cardId}")
    @Operation(
            summary = "Получить карту по ID",
            description = "ADMIN может получить любую карту, USER — только свою карту"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<CardResponse> getCardById(
            @AuthenticationPrincipal UserPrincipal user,
            @Parameter(description = "ID карты") @PathVariable UUID cardId
    ) {
        return ResponseEntity.ok(cardService.getCardByIdIfHaveAccess(cardId, user));
    }

    @GetMapping
    @Operation(summary = "Получить все карты", description = "Фильтрация карт по статусу, балансу и времени создания с пагинацией и поиском")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<CardResponse>> getAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) BigDecimal minBalance,
            @RequestParam(required = false) BigDecimal maxBalance,
            @Parameter(description = "Дата и время, начиная с которого искать",
                    schema = @Schema(type = "string", format = "date-time"))
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdAfter,

            @Parameter(description = "Дата и время, до которого искать",
                    schema = @Schema(type = "string", format = "date-time"))
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdBefore
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(cardService.getAllCards(pageable, search, status, minBalance, maxBalance, createdAfter, createdBefore));
    }

    @GetMapping("/my")
    @Operation(summary = "Получить мои карты", description = "Фильтрация своих карт по статусу, балансу и времени создания с пагинацией и поиском")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<CardResponse>> getMyCards(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) BigDecimal minBalance,
            @RequestParam(required = false) BigDecimal maxBalance,
            @Parameter(description = "Дата и время, начиная с которого искать",
                    schema = @Schema(type = "string", format = "date-time"))
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdAfter,

            @Parameter(description = "Дата и время, до которого искать",
                    schema = @Schema(type = "string", format = "date-time"))
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdBefore
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(cardService.getUserCards(
                user.getId(), pageable, search, status, minBalance, maxBalance, createdAfter, createdBefore));
    }


    @PostMapping
    @Operation(summary = "Создать новую карту", description = "Создание новой банковской карты для текущего пользователя")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CreateCardResponse> createCard(
            @Valid @RequestBody CreateCardRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                cardService.createCard(request)
        );
    }

    @PatchMapping("/{cardId}/block")
    @Operation(summary = "Заблокировать карту", description = "Блокировка указанной карты")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponse> blockCard(
            @Parameter(description = "uuid карты")
            @PathVariable UUID cardId) {

        return ResponseEntity.ok(cardService.blockCard(cardId));
    }

    @PatchMapping("/{cardId}/unblock")
    @Operation(summary = "Разблокировать карту", description = "Разблокировка указанной карты")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponse> unblockCard(
            @Parameter(description = "ID карты")
            @PathVariable UUID cardId) {

        return ResponseEntity.ok(cardService.unblockCard(cardId));
    }

    @PatchMapping("/{cardId}/activate")
    @Operation(summary = "Активировать карту", description = "Активация указанной карты")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponse> activateCard(
            @Parameter(description = "ID карты")
            @PathVariable UUID cardId) {

        return ResponseEntity.ok(cardService.activateCard(cardId));
    }

    @DeleteMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить карту", description = "Удаление указанной карты")
    public ResponseEntity<Void> deleteCard(
            @Parameter(description = "ID карты")
            @PathVariable UUID cardId){

        cardService.deleteCard(cardId);
        return ResponseEntity.ok().build();
    }
}
