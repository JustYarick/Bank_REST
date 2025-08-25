package com.example.bankcards.service;

import com.example.bankcards.dto.transfer.CreateTransferRequest;
import com.example.bankcards.entity.CardEntity;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.UserEntity;
import com.example.bankcards.exception.NotAllowedException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.service.impl.TransactionsServiceImpl;
import com.example.bankcards.util.CardEncryption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionsService Unit Tests")
class TransactionsServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardEncryption cardEncryption;

    @InjectMocks
    private TransactionsServiceImpl transactionsService;

    private UUID userId;
    private UUID otherUserId;
    private String fromCardNumber;
    private String toCardNumber;
    private String encryptedFromCardNumber;
    private String encryptedToCardNumber;
    private CardEntity fromCard;
    private CardEntity toCard;
    private UserEntity user;
    private UserEntity otherUser;
    private CreateTransferRequest request;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

        fromCardNumber = "4277011234567890";
        toCardNumber = "4277019876543210";
        encryptedFromCardNumber = "encrypted-from-card";
        encryptedToCardNumber = "encrypted-to-card";

        user = UserEntity.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .isActive(true)
                .build();

        otherUser = UserEntity.builder()
                .id(otherUserId)
                .username("otheruser")
                .email("other@example.com")
                .build();

        fromCard = CardEntity.builder()
                .id(UUID.randomUUID())
                .cardNumberEncrypted(encryptedFromCardNumber)
                .cardNumberMask("**** **** **** 7890")
                .holderName("TEST USER")
                .expirationDate(LocalDate.now().plusYears(4))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        toCard = CardEntity.builder()
                .id(UUID.randomUUID())
                .cardNumberEncrypted(encryptedToCardNumber)
                .cardNumberMask("**** **** **** 3210")
                .holderName("TEST USER")
                .expirationDate(LocalDate.now().plusYears(4))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        request = new CreateTransferRequest();
        request.setFromCardNumber(fromCardNumber);
        request.setToCardNumber(toCardNumber);
        request.setAmount(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Should create transaction successfully between same user cards")
    void createTransaction_WhenValidRequest_ShouldTransferMoney() {
        BigDecimal initialFromBalance = fromCard.getBalance();
        BigDecimal initialToBalance = toCard.getBalance();
        BigDecimal transferAmount = request.getAmount();

        when(cardEncryption.encryptCardNumber(fromCardNumber)).thenReturn(encryptedFromCardNumber);
        when(cardEncryption.encryptCardNumber(toCardNumber)).thenReturn(encryptedToCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedFromCardNumber))
                .thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberEncrypted(encryptedToCardNumber))
                .thenReturn(Optional.of(toCard));

        transactionsService.createTransaction(request, userId);

        assertThat(fromCard.getBalance()).isEqualTo(initialFromBalance.subtract(transferAmount));
        assertThat(toCard.getBalance()).isEqualTo(initialToBalance.add(transferAmount));

        verify(cardRepository).save(fromCard);
        verify(cardRepository).save(toCard);
    }

    @Test
    @DisplayName("Should throw NotFoundException when from card not found")
    void createTransaction_WhenFromCardNotFound_ShouldThrowNotFoundException() {
        when(cardEncryption.encryptCardNumber(fromCardNumber)).thenReturn(encryptedFromCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedFromCardNumber))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionsService.createTransaction(request, userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Card number not found");

        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when to card not found")
    void createTransaction_WhenToCardNotFound_ShouldThrowNotFoundException() {
        when(cardEncryption.encryptCardNumber(fromCardNumber)).thenReturn(encryptedFromCardNumber);
        when(cardEncryption.encryptCardNumber(toCardNumber)).thenReturn(encryptedToCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedFromCardNumber))
                .thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberEncrypted(encryptedToCardNumber))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionsService.createTransaction(request, userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Card number not found");

        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NotAllowedException when user is not owner of from card")
    void createTransaction_WhenUserNotOwnerOfFromCard_ShouldThrowNotAllowedException() {
        fromCard.setUser(otherUser);

        when(cardEncryption.encryptCardNumber(fromCardNumber)).thenReturn(encryptedFromCardNumber);
        when(cardEncryption.encryptCardNumber(toCardNumber)).thenReturn(encryptedToCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedFromCardNumber))
                .thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberEncrypted(encryptedToCardNumber))
                .thenReturn(Optional.of(toCard));

        assertThatThrownBy(() -> transactionsService.createTransaction(request, userId))
                .isInstanceOf(NotAllowedException.class)
                .hasMessageContaining("You are not owner of this card");

        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NotAllowedException when cards belong to different users")
    void createTransaction_WhenDifferentCardOwners_ShouldThrowNotAllowedException() {
        toCard.setUser(otherUser);

        when(cardEncryption.encryptCardNumber(fromCardNumber)).thenReturn(encryptedFromCardNumber);
        when(cardEncryption.encryptCardNumber(toCardNumber)).thenReturn(encryptedToCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedFromCardNumber))
                .thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberEncrypted(encryptedToCardNumber))
                .thenReturn(Optional.of(toCard));

        assertThatThrownBy(() -> transactionsService.createTransaction(request, userId))
                .isInstanceOf(NotAllowedException.class)
                .hasMessageContaining("Transactions only between your own cards allowed");

        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NotAllowedException when from card is not active")
    void createTransaction_WhenFromCardNotActive_ShouldThrowNotAllowedException() {
        fromCard.setStatus(CardStatus.BLOCKED);

        when(cardEncryption.encryptCardNumber(fromCardNumber)).thenReturn(encryptedFromCardNumber);
        when(cardEncryption.encryptCardNumber(toCardNumber)).thenReturn(encryptedToCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedFromCardNumber))
                .thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberEncrypted(encryptedToCardNumber))
                .thenReturn(Optional.of(toCard));

        assertThatThrownBy(() -> transactionsService.createTransaction(request, userId))
                .isInstanceOf(NotAllowedException.class)
                .hasMessageContaining("One of cards isn't active");

        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NotAllowedException when to card is not active")
    void createTransaction_WhenToCardNotActive_ShouldThrowNotAllowedException() {
        toCard.setStatus(CardStatus.EXPIRED);

        when(cardEncryption.encryptCardNumber(fromCardNumber)).thenReturn(encryptedFromCardNumber);
        when(cardEncryption.encryptCardNumber(toCardNumber)).thenReturn(encryptedToCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedFromCardNumber))
                .thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberEncrypted(encryptedToCardNumber))
                .thenReturn(Optional.of(toCard));

        assertThatThrownBy(() -> transactionsService.createTransaction(request, userId))
                .isInstanceOf(NotAllowedException.class)
                .hasMessageContaining("One of cards isn't active");

        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NotAllowedException when insufficient balance")
    void createTransaction_WhenInsufficientBalance_ShouldThrowNotAllowedException() {
        fromCard.setBalance(new BigDecimal("50.00"));

        when(cardEncryption.encryptCardNumber(fromCardNumber)).thenReturn(encryptedFromCardNumber);
        when(cardEncryption.encryptCardNumber(toCardNumber)).thenReturn(encryptedToCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedFromCardNumber))
                .thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberEncrypted(encryptedToCardNumber))
                .thenReturn(Optional.of(toCard));

        assertThatThrownBy(() -> transactionsService.createTransaction(request, userId))
                .isInstanceOf(NotAllowedException.class)
                .hasMessageContaining("Insufficient balance on source card");

        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle exact balance transfer")
    void createTransaction_WhenExactBalance_ShouldTransferSuccessfully() {
        BigDecimal exactBalance = new BigDecimal("1000.00");
        fromCard.setBalance(exactBalance);
        request.setAmount(exactBalance);

        when(cardEncryption.encryptCardNumber(fromCardNumber)).thenReturn(encryptedFromCardNumber);
        when(cardEncryption.encryptCardNumber(toCardNumber)).thenReturn(encryptedToCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedFromCardNumber))
                .thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberEncrypted(encryptedToCardNumber))
                .thenReturn(Optional.of(toCard));

        transactionsService.createTransaction(request, userId);

        assertThat(fromCard.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(toCard.getBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));

        verify(cardRepository).save(fromCard);
        verify(cardRepository).save(toCard);
    }


    @Test
    @DisplayName("Should handle small decimal amounts")
    void createTransaction_WhenSmallDecimalAmount_ShouldTransferCorrectly() {
        BigDecimal smallAmount = new BigDecimal("0.01");
        request.setAmount(smallAmount);

        BigDecimal initialFromBalance = fromCard.getBalance();
        BigDecimal initialToBalance = toCard.getBalance();

        when(cardEncryption.encryptCardNumber(fromCardNumber)).thenReturn(encryptedFromCardNumber);
        when(cardEncryption.encryptCardNumber(toCardNumber)).thenReturn(encryptedToCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedFromCardNumber))
                .thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberEncrypted(encryptedToCardNumber))
                .thenReturn(Optional.of(toCard));

        transactionsService.createTransaction(request, userId);

        assertThat(fromCard.getBalance()).isEqualTo(initialFromBalance.subtract(smallAmount));
        assertThat(toCard.getBalance()).isEqualTo(initialToBalance.add(smallAmount));

        verify(cardRepository, times(2)).save(any(CardEntity.class));
    }

    @Test
    @DisplayName("Should encrypt both card numbers correctly")
    void createTransaction_WhenCalled_ShouldEncryptBothCardNumbers() {
        when(cardEncryption.encryptCardNumber(fromCardNumber)).thenReturn(encryptedFromCardNumber);
        when(cardEncryption.encryptCardNumber(toCardNumber)).thenReturn(encryptedToCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedFromCardNumber))
                .thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberEncrypted(encryptedToCardNumber))
                .thenReturn(Optional.of(toCard));

        transactionsService.createTransaction(request, userId);

        verify(cardEncryption).encryptCardNumber(fromCardNumber);
        verify(cardEncryption).encryptCardNumber(toCardNumber);
    }

    @Test
    @DisplayName("Should handle transfer between same card numbers")
    void createTransaction_WhenSameCardNumbers_ShouldAllowTransfer() {
        request.setToCardNumber(fromCardNumber);

        when(cardEncryption.encryptCardNumber(fromCardNumber))
                .thenReturn(encryptedFromCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedFromCardNumber))
                .thenReturn(Optional.of(fromCard));

        transactionsService.createTransaction(request, userId);

        verify(cardRepository, times(2)).save(fromCard);
    }

    @Test
    @DisplayName("Should save both cards in correct order")
    void createTransaction_WhenCalled_ShouldSaveBothCards() {
        when(cardEncryption.encryptCardNumber(fromCardNumber)).thenReturn(encryptedFromCardNumber);
        when(cardEncryption.encryptCardNumber(toCardNumber)).thenReturn(encryptedToCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedFromCardNumber))
                .thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberEncrypted(encryptedToCardNumber))
                .thenReturn(Optional.of(toCard));

        transactionsService.createTransaction(request, userId);

        verify(cardRepository).save(fromCard);
        verify(cardRepository).save(toCard);
        verify(cardRepository, times(2)).save(any(CardEntity.class));
    }
}
