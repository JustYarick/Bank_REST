package com.example.bankcards.service.impl;

import com.example.bankcards.dto.transfer.CreateTransferRequest;
import com.example.bankcards.entity.CardEntity;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.NotAllowedException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.service.interfaces.TransactionsService;
import com.example.bankcards.util.CardEncryption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionsServiceImpl implements TransactionsService {

    private final CardRepository cardRepository;
    private final CardEncryption cardEncryption;

    @Override
    @Transactional
    public void createTransaction(CreateTransferRequest request, UUID userId) {
        CardEntity fromCard = getCardByCardNumber(request.getFromCardNumber());
        CardEntity toCard   = getCardByCardNumber(request.getToCardNumber());

        if (!fromCard.getUser().getId().equals(userId)) {
            throw new NotAllowedException("You are not owner of this card");
        }
        if (!Objects.equals(fromCard.getUser().getId(), toCard.getUser().getId())) {
            throw new NotAllowedException("Transactions only between your own cards allowed");
        }
        if (fromCard.getStatus() != CardStatus.ACTIVE || toCard.getStatus() != CardStatus.ACTIVE) {
            throw new NotAllowedException("One of cards isn't active");
        }
        if (fromCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new NotAllowedException("Insufficient balance on source card");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
        toCard  .setBalance(toCard.getBalance().add(request.getAmount()));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);
    }

    private CardEntity getCardByCardNumber(String cardNumber) {
        String encrypted = cardEncryption.encryptCardNumber(cardNumber);
        return cardRepository.findByCardNumberEncrypted(encrypted)
                .orElseThrow(() -> new NotFoundException("Card number not found"));
    }
}
