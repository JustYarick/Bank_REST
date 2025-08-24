package com.example.bankcards.service.interfaces;

import com.example.bankcards.dto.transfer.CreateTransferRequest;

import java.util.UUID;

public interface TransactionsService {
    void createTransaction(CreateTransferRequest request, UUID userId);
}
