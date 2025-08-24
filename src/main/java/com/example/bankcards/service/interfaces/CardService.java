package com.example.bankcards.service.interfaces;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.CreateCardResponse;
import com.example.bankcards.dto.core.PagedResponse;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.security.UserPrincipal;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface CardService {

    CreateCardResponse createCard(CreateCardRequest request);
    CardResponse blockCard(UUID cardId);
    CardResponse unblockCard(UUID cardId);
    CardResponse activateCard(UUID cardId);
    void deleteCard(UUID cardId);

    PagedResponse<CardResponse> getAllCards(
            Pageable pageable,
            String search,
            CardStatus status,
            BigDecimal minBalance,
            BigDecimal maxBalance,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore
    );

    PagedResponse<CardResponse> getUserCards(
            UUID userId,
            Pageable pageable,
            String search,
            CardStatus status,
            BigDecimal minBalance,
            BigDecimal maxBalance,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore
    );

    CardResponse getCardByIdIfHaveAccess(UUID cardId, UserPrincipal user);
}
