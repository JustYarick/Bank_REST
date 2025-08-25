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
public class CreateCardResponse {

    private UUID id;
    private String cardNumber;
    private String holderName;
    private LocalDate expirationDate;
    private CardStatus status;
    private BigDecimal balance;
    private LocalDateTime createdAt;

    public static CreateCardResponse convert(CardEntity card, String number) {
        return CreateCardResponse.builder()
                .id(card.getId())
                .cardNumber(number)
                .holderName(card.getHolderName())
                .expirationDate(card.getExpirationDate())
                .status(card.getStatus())
                .balance(card.getBalance())
                .createdAt(card.getCreatedAt())
                .build();
    }
}