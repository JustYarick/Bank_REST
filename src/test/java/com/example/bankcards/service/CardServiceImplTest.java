package com.example.bankcards.service;

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
import com.example.bankcards.service.impl.CardServiceImpl;
import com.example.bankcards.util.CardEncryption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardService Unit Tests")
class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardEncryption cardEncryption;

    @InjectMocks
    private CardServiceImpl cardService;

    private UUID cardId;
    private UUID userId;
    private CardEntity cardEntity;
    private UserEntity userEntity;
    private UserPrincipal userPrincipal;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        cardId = UUID.randomUUID();
        userId = UUID.randomUUID();

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
                .cardNumberEncrypted("encrypted-number")
                .cardNumberMask("**** **** **** 1234")
                .holderName("TEST USER")
                .expirationDate(LocalDate.now().plusYears(4))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .createdAt(LocalDateTime.now())
                .user(userEntity)
                .build();

        userPrincipal = new UserPrincipal(
                userId,
                "testuser",
                "test@example.com",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                true
        );

        adminPrincipal = new UserPrincipal(
                UUID.randomUUID(),
                "admin",
                "admin@example.com",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")),
                true
        );
    }

    @Test
    @DisplayName("Should get card by ID when user has access")
    void getCardByIdIfHaveAccess_WhenUserHasAccess_ShouldReturnCard() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(cardEntity));

        CardResponse result = cardService.getCardByIdIfHaveAccess(cardId, userPrincipal);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(cardId);
        assertThat(result.getCardNumberMask()).isEqualTo("**** **** **** 1234");
        assertThat(result.getHolderName()).isEqualTo("TEST USER");
    }

    @Test
    @DisplayName("Should get card by ID when admin requests any card")
    void getCardByIdIfHaveAccess_WhenAdminUser_ShouldReturnCard() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(cardEntity));

        CardResponse result = cardService.getCardByIdIfHaveAccess(cardId, adminPrincipal);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(cardId);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when user tries to access other user's card")
    void getCardByIdIfHaveAccess_WhenUserAccessesOtherCard_ShouldThrowAccessDeniedException() {
        UUID otherUserId = UUID.randomUUID();
        UserEntity otherUser = UserEntity.builder().id(otherUserId).build();
        cardEntity.setUser(otherUser);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(cardEntity));

        assertThatThrownBy(() -> cardService.getCardByIdIfHaveAccess(cardId, userPrincipal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied to card: " + cardId);
    }

    @Test
    @DisplayName("Should throw NotFoundException when card does not exist")
    void getCardByIdIfHaveAccess_WhenCardNotFound_ShouldThrowNotFoundException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCardByIdIfHaveAccess(cardId, userPrincipal))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Card not found with id: " + cardId);
    }

    @Test
    @DisplayName("Should create card successfully")
    void createCard_WhenValidRequest_ShouldCreateCard() {
        CreateCardRequest request = new CreateCardRequest();
        request.setCardUserUuid(userId);
        request.setHolderName("John Doe");
        request.setInitialBalance(new BigDecimal("500.00"));

        String encryptedNumber = "encrypted-4277011234567890";

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(cardEncryption.encryptCardNumber(any())).thenReturn(encryptedNumber);
        when(cardRepository.existsByCardNumberEncrypted(any())).thenReturn(false);
        when(cardRepository.save(any(CardEntity.class))).thenAnswer(invocation -> {
            CardEntity card = invocation.getArgument(0);
            card.setId(cardId);
            return card;
        });

        CreateCardResponse result = cardService.createCard(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(cardId);
        assertThat(result.getHolderName()).isEqualTo("JOHN DOE");
        assertThat(result.getBalance()).isEqualTo(new BigDecimal("500.00"));
        assertThat(result.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(result.getExpirationDate()).isEqualTo(LocalDate.now().plusYears(4));

        verify(cardRepository).save(any(CardEntity.class));
        verify(cardEncryption, times(2)).encryptCardNumber(any());
        verify(cardRepository).existsByCardNumberEncrypted(any());
    }


    @Test
    @DisplayName("Should throw NotFoundException when user not found during card creation")
    void createCard_WhenUserNotFound_ShouldThrowNotFoundException() {
        CreateCardRequest request = new CreateCardRequest();
        request.setCardUserUuid(userId);
        request.setHolderName("John Doe");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.createCard(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found with id: " + userId);
    }

    @Test
    @DisplayName("Should get all cards with pagination")
    void getAllCards_WhenCalled_ShouldReturnPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        List<CardEntity> cards = List.of(cardEntity);
        Page<CardEntity> page = new PageImpl<>(cards, pageable, 1);

        when(cardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PagedResponse<CardResponse> result = cardService.getAllCards(
                pageable, null, null, null, null, null, null
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should get user cards with pagination")
    void getUserCards_WhenCalled_ShouldReturnPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        List<CardEntity> cards = List.of(cardEntity);
        Page<CardEntity> page = new PageImpl<>(cards, pageable, 1);

        when(cardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PagedResponse<CardResponse> result = cardService.getUserCards(
                userId, pageable, null, null, null, null, null, null
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should block card successfully")
    void blockCard_WhenValidCard_ShouldBlockCard() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(cardEntity));
        when(cardRepository.save(any(CardEntity.class))).thenReturn(cardEntity);

        CardResponse result = cardService.blockCard(cardId);

        assertThat(result).isNotNull();
        verify(cardRepository).save(cardEntity);
        assertThat(cardEntity.getStatus()).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    @DisplayName("Should unblock card successfully")
    void unblockCard_WhenValidCard_ShouldUnblockCard() {
        cardEntity.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(cardEntity));
        when(cardRepository.save(any(CardEntity.class))).thenReturn(cardEntity);

        CardResponse result = cardService.unblockCard(cardId);

        assertThat(result).isNotNull();
        verify(cardRepository).save(cardEntity);
        assertThat(cardEntity.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when trying to unblock expired card")
    void unblockCard_WhenCardExpired_ShouldThrowIllegalStateException() {
        cardEntity.setStatus(CardStatus.EXPIRED);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(cardEntity));

        assertThatThrownBy(() -> cardService.unblockCard(cardId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot unblock expired card");
    }

    @Test
    @DisplayName("Should activate card successfully")
    void activateCard_WhenValidCard_ShouldActivateCard() {
        cardEntity.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(cardEntity));
        when(cardRepository.save(any(CardEntity.class))).thenReturn(cardEntity);

        CardResponse result = cardService.activateCard(cardId);

        assertThat(result).isNotNull();
        verify(cardRepository).save(cardEntity);
        assertThat(cardEntity.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should delete card successfully")
    void deleteCard_WhenValidCard_ShouldDeleteCard() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(cardEntity));

        cardService.deleteCard(cardId);

        verify(cardRepository).delete(cardEntity);
    }

    @Test
    @DisplayName("Should throw NotFoundException when trying to delete non-existent card")
    void deleteCard_WhenCardNotFound_ShouldThrowNotFoundException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.deleteCard(cardId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Card not found with id: " + cardId);
    }
}
