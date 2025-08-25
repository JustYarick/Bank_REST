package com.example.bankcards.service.impl;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.CreateCardResponse;
import com.example.bankcards.dto.core.PagedResponse;
import com.example.bankcards.entity.CardEntity;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.UserEntity;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.interfaces.CardService;
import com.example.bankcards.util.CardEncryption;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardEncryption cardEncryption;

    private static final String CARD_BIN = "427701";
    private static final int CARD_NUMBER_LENGTH = 16;
    private static final int CARD_VALIDITY_YEARS = 4;

    @Override
    public CardResponse getCardByIdIfHaveAccess(UUID cardId, UserPrincipal user){
        CardEntity card = findCard(cardId);

        if (user.getAuthorities().stream()
                .anyMatch(
                        auth -> "ROLE_USER".equals(auth.getAuthority())
                ) && !card.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied to card: " + cardId);
        }
        return CardResponse.convert(card);
    }

    @Override
    @Transactional
    public CreateCardResponse createCard(CreateCardRequest request) {
        UUID userUuid = request.getCardUserUuid();
        UserEntity user = userRepository.findById(userUuid)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userUuid));

        String cardNumber = generateUniqueCardNumber();
        CardEntity card = CardEntity.builder()
                .cardNumberEncrypted(cardEncryption.encryptCardNumber(cardNumber))
                .cardNumberMask(createCardMask(cardNumber))
                .holderName(request.getHolderName().toUpperCase())
                .expirationDate(LocalDate.now().plusYears(CARD_VALIDITY_YEARS))
                .status(CardStatus.ACTIVE)
                .balance(request.getInitialBalance())
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        CardEntity saved = cardRepository.save(card);
        log.info("Created new card for user {}: {}", userUuid, saved.getId());
        return CreateCardResponse.convert(saved, cardNumber);
    }

    @Override
    public PagedResponse<CardResponse> getAllCards(
            Pageable pageable,
            String search,
            CardStatus status,
            BigDecimal minBalance,
            BigDecimal maxBalance,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore
    ) {
        Specification<CardEntity> spec = buildSpecification(
                null, search, status, minBalance, maxBalance, createdAfter, createdBefore
        );
        Page<CardEntity> page = cardRepository.findAll(spec, pageable);
        return toPagedResponse(page);
    }

    @Override
    public PagedResponse<CardResponse> getUserCards(
            UUID userId,
            Pageable pageable,
            String search,
            CardStatus status,
            BigDecimal minBalance,
            BigDecimal maxBalance,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore
    ) {
        Specification<CardEntity> spec = buildSpecification(
                userId, search, status, minBalance, maxBalance, createdAfter, createdBefore
        );
        Page<CardEntity> page = cardRepository.findAll(spec, pageable);
        return toPagedResponse(page);
    }

    @Override
    @Transactional
    public CardResponse blockCard(UUID cardId) {
        CardEntity card = findCard(cardId);
        card.setStatus(CardStatus.BLOCKED);
        CardEntity saved = cardRepository.save(card);
        log.info("Card {} blocked", cardId);
        return CardResponse.convert(saved);
    }

    @Override
    @Transactional
    public CardResponse unblockCard(UUID cardId) {
        CardEntity card = findCard(cardId);
        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new IllegalStateException("Cannot unblock expired card");
        }
        card.setStatus(CardStatus.ACTIVE);
        CardEntity saved = cardRepository.save(card);
        log.info("Card {} unblocked", cardId);
        return CardResponse.convert(saved);
    }

    @Override
    @Transactional
    public CardResponse activateCard(UUID cardId) {
        CardEntity card = findCard(cardId);
        card.setStatus(CardStatus.ACTIVE);
        CardEntity saved = cardRepository.save(card);
        log.info("Card {} activated", cardId);
        return CardResponse.convert(saved);
    }

    @Override
    @Transactional
    public void deleteCard(UUID cardId) {
        CardEntity card = findCard(cardId);
        cardRepository.delete(card);
        log.info("Card {} deleted", cardId);
    }

    private CardEntity findCard(UUID cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found with id: " + cardId));
    }

    private Specification<CardEntity> buildSpecification(
            UUID userId,
            String search,
            CardStatus status,
            BigDecimal minBalance,
            BigDecimal maxBalance,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (StringUtils.hasText(search)) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("holderName")), like),
                        cb.like(root.get("cardNumberMask"), "%" + search + "%")
                ));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (minBalance != null) {
                predicates.add(cb.ge(root.get("balance"), minBalance));
            }
            if (maxBalance != null) {
                predicates.add(cb.le(root.get("balance"), maxBalance));
            }
            if (createdAfter != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfter));
            }
            if (createdBefore != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdBefore));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private PagedResponse<CardResponse> toPagedResponse(Page<CardEntity> page) {
        List<CardResponse> list = page.getContent().stream()
                .map(CardResponse::convert)
                .toList();
        return PagedResponse.<CardResponse>builder()
                .content(list)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private String generateUniqueCardNumber() {
        SecureRandom random = new SecureRandom();
        String number;
        do {
            StringBuilder sb = new StringBuilder(CARD_BIN);
            for (int i = 0; i < CARD_NUMBER_LENGTH - CARD_BIN.length(); i++) {
                sb.append(random.nextInt(10));
            }
            number = sb.toString();
        } while (cardRepository.existsByCardNumberEncrypted(cardEncryption.encryptCardNumber(number)));
        return number;
    }

    private String createCardMask(String number) {
        if (number.length() != 16) {
            throw new IllegalArgumentException("Card number must be 16 digits");
        }
        return "**** **** **** " + number.substring(12);
    }
}
