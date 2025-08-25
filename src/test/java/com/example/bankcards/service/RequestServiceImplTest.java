package com.example.bankcards.service;

import com.example.bankcards.dto.requests.CardBlockRequest;
import com.example.bankcards.entity.CardEntity;
import com.example.bankcards.entity.RequestEntity;
import com.example.bankcards.entity.RequestStatus;
import com.example.bankcards.entity.UserEntity;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.RequestRepository;
import com.example.bankcards.service.impl.RequestServiceImpl;
import com.example.bankcards.util.CardEncryption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestService Unit Tests")
class RequestServiceImplTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardEncryption cardEncryption;

    @InjectMocks
    private RequestServiceImpl requestService;

    private UUID userId;
    private UUID cardId;
    private String cardNumber;
    private String encryptedCardNumber;
    private CardEntity cardEntity;
    private UserEntity userEntity;
    private CardBlockRequest request;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        cardId = UUID.randomUUID();
        cardNumber = "4277011234567890";
        encryptedCardNumber = "encrypted-card-number";

        userEntity = UserEntity.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .isActive(true)
                .build();

        cardEntity = CardEntity.builder()
                .id(cardId)
                .cardNumberEncrypted(encryptedCardNumber)
                .cardNumberMask("**** **** **** 7890")
                .holderName("TEST USER")
                .user(userEntity)
                .build();

        request = new CardBlockRequest();
        request.setCardNumber(cardNumber);
        request.setReason("Lost card");
    }

    @Test
    @DisplayName("Should create card block request successfully")
    void createCardBlockRequest_WhenValidRequest_ShouldCreateRequest() {
        when(cardEncryption.encryptCardNumber(cardNumber)).thenReturn(encryptedCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedCardNumber))
                .thenReturn(Optional.of(cardEntity));
        when(requestRepository.save(any(RequestEntity.class)))
                .thenAnswer(invocation -> {
                    RequestEntity req = invocation.getArgument(0);
                    req.setId(UUID.randomUUID());
                    req.setCreatedAt(LocalDateTime.now());
                    req.setUpdatedAt(LocalDateTime.now());
                    return req;
                });

        requestService.createCardBlockRequest(request, userId);

        verify(cardEncryption).encryptCardNumber(cardNumber);
        verify(cardRepository).findByCardNumberEncrypted(encryptedCardNumber);
        verify(requestRepository).save(argThat(savedRequest -> {
            assertThat(savedRequest.getCard()).isEqualTo(cardEntity);
            assertThat(savedRequest.getReason()).isEqualTo("Lost card");
            assertThat(savedRequest.getStatus()).isEqualTo(RequestStatus.NEW);
            return true;
        }));
    }

    @Test
    @DisplayName("Should create request with default empty reason")
    void createCardBlockRequest_WhenNoReasonProvided_ShouldUseDefaultReason() {
        CardBlockRequest requestWithoutReason = new CardBlockRequest();
        requestWithoutReason.setCardNumber(cardNumber);

        when(cardEncryption.encryptCardNumber(cardNumber)).thenReturn(encryptedCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedCardNumber))
                .thenReturn(Optional.of(cardEntity));

        requestService.createCardBlockRequest(requestWithoutReason, userId);

        verify(requestRepository).save(argThat(savedRequest -> {
            assertThat(savedRequest.getReason()).isEqualTo("");
            assertThat(savedRequest.getCard()).isEqualTo(cardEntity);
            assertThat(savedRequest.getStatus()).isEqualTo(RequestStatus.NEW);
            return true;
        }));
    }

    @Test
    @DisplayName("Should create request with null reason handled gracefully")
    void createCardBlockRequest_WhenNullReason_ShouldHandleGracefully() {
        request.setReason(null);

        when(cardEncryption.encryptCardNumber(cardNumber)).thenReturn(encryptedCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedCardNumber))
                .thenReturn(Optional.of(cardEntity));

        requestService.createCardBlockRequest(request, userId);

        verify(requestRepository).save(argThat(savedRequest -> {
            assertThat(savedRequest.getReason()).isNull();
            assertThat(savedRequest.getCard()).isEqualTo(cardEntity);
            assertThat(savedRequest.getStatus()).isEqualTo(RequestStatus.NEW);
            return true;
        }));
    }

    @Test
    @DisplayName("Should throw NotFoundException when card not found")
    void createCardBlockRequest_WhenCardNotFound_ShouldThrowNotFoundException() {
        when(cardEncryption.encryptCardNumber(cardNumber)).thenReturn(encryptedCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedCardNumber))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.createCardBlockRequest(request, userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Card Not Found");

        verify(cardEncryption).encryptCardNumber(cardNumber);
        verify(cardRepository).findByCardNumberEncrypted(encryptedCardNumber);
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when user tries to block other user's card")
    void createCardBlockRequest_WhenNotUserCard_ShouldThrowAccessDeniedException() {
        UUID otherUserId = UUID.randomUUID();
        UserEntity otherUser = UserEntity.builder()
                .id(otherUserId)
                .username("otheruser")
                .build();
        cardEntity.setUser(otherUser);

        when(cardEncryption.encryptCardNumber(cardNumber)).thenReturn(encryptedCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedCardNumber))
                .thenReturn(Optional.of(cardEntity));

        assertThatThrownBy(() -> requestService.createCardBlockRequest(request, userId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access Denied, not your card");

        verify(cardEncryption).encryptCardNumber(cardNumber);
        verify(cardRepository).findByCardNumberEncrypted(encryptedCardNumber);
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should use correct encryption for card lookup")
    void createCardBlockRequest_WhenCalled_ShouldEncryptCardNumberCorrectly() {
        String specificCardNumber = "1234567890123456";
        String specificEncryptedNumber = "specific-encrypted-number";
        request.setCardNumber(specificCardNumber);

        when(cardEncryption.encryptCardNumber(specificCardNumber)).thenReturn(specificEncryptedNumber);
        when(cardRepository.findByCardNumberEncrypted(specificEncryptedNumber))
                .thenReturn(Optional.of(cardEntity));

        requestService.createCardBlockRequest(request, userId);

        verify(cardEncryption).encryptCardNumber(specificCardNumber);
        verify(cardRepository).findByCardNumberEncrypted(specificEncryptedNumber);
        verify(requestRepository).save(any(RequestEntity.class));
    }

    @Test
    @DisplayName("Should set correct status for new request")
    void createCardBlockRequest_WhenCalled_ShouldSetNewStatus() {
        when(cardEncryption.encryptCardNumber(cardNumber)).thenReturn(encryptedCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedCardNumber))
                .thenReturn(Optional.of(cardEntity));

        requestService.createCardBlockRequest(request, userId);

        verify(requestRepository).save(argThat(savedRequest -> {
            assertThat(savedRequest.getStatus()).isEqualTo(RequestStatus.NEW);
            return true;
        }));
    }

    @Test
    @DisplayName("Should handle long reason text")
    void createCardBlockRequest_WhenLongReason_ShouldHandleCorrectly() {
        String longReason = "A".repeat(255);
        request.setReason(longReason);

        when(cardEncryption.encryptCardNumber(cardNumber)).thenReturn(encryptedCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedCardNumber))
                .thenReturn(Optional.of(cardEntity));

        requestService.createCardBlockRequest(request, userId);

        verify(requestRepository).save(argThat(savedRequest -> {
            assertThat(savedRequest.getReason()).isEqualTo(longReason);
            assertThat(savedRequest.getReason().length()).isEqualTo(255);
            return true;
        }));
    }

    @Test
    @DisplayName("Should verify user ownership correctly")
    void createCardBlockRequest_WhenSameUser_ShouldAllowRequest() {
        when(cardEncryption.encryptCardNumber(cardNumber)).thenReturn(encryptedCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedCardNumber))
                .thenReturn(Optional.of(cardEntity));

        assertThatCode(() -> requestService.createCardBlockRequest(request, userId))
                .doesNotThrowAnyException();

        verify(requestRepository).save(any(RequestEntity.class));
    }

    @Test
    @DisplayName("Should handle special characters in reason")
    void createCardBlockRequest_WhenSpecialCharactersInReason_ShouldHandleCorrectly() {
        String reasonWithSpecialChars = "Потерял карту! @#$%^&*()_+ тест";
        request.setReason(reasonWithSpecialChars);

        when(cardEncryption.encryptCardNumber(cardNumber)).thenReturn(encryptedCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedCardNumber))
                .thenReturn(Optional.of(cardEntity));

        requestService.createCardBlockRequest(request, userId);

        verify(requestRepository).save(argThat(savedRequest -> {
            assertThat(savedRequest.getReason()).isEqualTo(reasonWithSpecialChars);
            return true;
        }));
    }

    @Test
    @DisplayName("Should not save when card lookup fails")
    void createCardBlockRequest_WhenCardLookupFails_ShouldNotSave() {
        when(cardEncryption.encryptCardNumber(cardNumber)).thenReturn(encryptedCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedCardNumber))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.createCardBlockRequest(request, userId))
                .isInstanceOf(NotFoundException.class);

        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should not save when access is denied")
    void createCardBlockRequest_WhenAccessDenied_ShouldNotSave() {
        UUID differentUserId = UUID.randomUUID();
        UserEntity differentUser = UserEntity.builder().id(differentUserId).build();
        cardEntity.setUser(differentUser);

        when(cardEncryption.encryptCardNumber(cardNumber)).thenReturn(encryptedCardNumber);
        when(cardRepository.findByCardNumberEncrypted(encryptedCardNumber))
                .thenReturn(Optional.of(cardEntity));

        assertThatThrownBy(() -> requestService.createCardBlockRequest(request, userId))
                .isInstanceOf(AccessDeniedException.class);

        verify(requestRepository, never()).save(any());
    }
}
