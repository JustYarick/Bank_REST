package com.example.bankcards.dto.card;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateCardRequest {

    @NotBlank(message = "Holder name is required")
    @Size(min = 2, max = 100, message = "Holder name must be between 2 and 100 characters")
    private String holderName;

    private BigDecimal initialBalance = BigDecimal.ZERO;

    @NotBlank(message = "user uuid is required")
    private UUID cardUserUuid;

    private String currencyCode = "RUB";
}