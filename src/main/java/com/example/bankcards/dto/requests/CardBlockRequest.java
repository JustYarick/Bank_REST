package com.example.bankcards.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CardBlockRequest {

    @NotBlank(message = "Card number is required")
    String cardNumber;
    String reason = "";
}
