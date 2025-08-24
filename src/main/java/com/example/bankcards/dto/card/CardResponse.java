package com.example.bankcards.dto.card;

import com.example.bankcards.entity.CardEntity;
import com.example.bankcards.entity.CardStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {

    private UUID id;
    private String cardNumberMask;
    private String holderName;
    private LocalDate expirationDate;
    private CardStatus status;
    private BigDecimal balance;
    private String currencyCode;
    private LocalDateTime createdAt;

    public static CardResponse convert(CardEntity card) {
        return CardResponse.builder()
                .id(card.getId())
                .cardNumberMask(card.getCardNumberMask())
                .holderName(card.getHolderName())
                .expirationDate(card.getExpirationDate())
                .status(card.getStatus())
                .balance(card.getBalance())
                .currencyCode(card.getCurrencyCode())
                .createdAt(card.getCreatedAt())
                .build();
    }
}