package com.example.bankcards.service.impl;

import com.example.bankcards.dto.requests.CardBlockRequest;
import com.example.bankcards.entity.CardEntity;
import com.example.bankcards.entity.RequestEntity;
import com.example.bankcards.entity.RequestStatus;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.RequestRepository;
import com.example.bankcards.service.interfaces.RequestService;
import com.example.bankcards.util.CardEncryption;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final CardRepository cardRepository;
    private final CardEncryption cardEncryption;

    @Transactional
    public void createCardBlockRequest(CardBlockRequest request, UUID id){

        CardEntity card = cardRepository.findByCardNumberEncrypted
                (cardEncryption.encryptCardNumber(request.getCardNumber()))
                .orElseThrow(()-> new NotFoundException("Card Not Found"));

        if (!card.getUser().getId().equals(id)){
            throw new AccessDeniedException("Access Denied, not your card");
        }

        RequestEntity newRequest =  RequestEntity.builder()
                .card(card)
                .reason(request.getReason())
                .status(RequestStatus.NEW)
                .build();

        requestRepository.save(newRequest);
    }
}
