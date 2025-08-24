package com.example.bankcards.service.interfaces;

import com.example.bankcards.dto.requests.CardBlockRequest;

import java.util.UUID;

public interface RequestService {
    void createCardBlockRequest(CardBlockRequest request, UUID id);
}
