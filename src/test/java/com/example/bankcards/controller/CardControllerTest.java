package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.CreateCardResponse;
import com.example.bankcards.dto.core.PagedResponse;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.DefaultExceptionHandler;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.interfaces.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardController Unit Tests")
class CardControllerTest {

    @Mock
    private CardService cardService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID cardId;
    private UUID userId;
    private CardResponse cardResponse;
    private CreateCardRequest createCardRequest;
    private CreateCardResponse createCardResponse;
    private PagedResponse<CardResponse> pagedResponse;

    @BeforeEach
    void setUp() {
        CardController cardController = new CardController(cardService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(cardController)
                .setControllerAdvice(new DefaultExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();

        cardId = UUID.randomUUID();
        userId = UUID.randomUUID();

        cardResponse = CardResponse.builder()
                .id(cardId)
                .cardNumberMask("**** **** **** 1234")
                .holderName("TEST USER")
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .createdAt(LocalDateTime.now())
                .build();

        createCardRequest = new CreateCardRequest();
        createCardRequest.setCardUserUuid(userId);
        createCardRequest.setHolderName("John Doe");
        createCardRequest.setInitialBalance(new BigDecimal("500.00"));

        createCardResponse = CreateCardResponse.builder()
                .id(cardId)
                .cardNumber("4277011234567890")
                .holderName("JOHN DOE")
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .createdAt(LocalDateTime.now())
                .build();

        pagedResponse = PagedResponse.<CardResponse>builder()
                .content(List.of(cardResponse))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();
    }

    @Test
    @DisplayName("Should get card by ID successfully")
    void getCardById_WhenValidId_ShouldReturnCard() throws Exception {
        when(cardService.getCardByIdIfHaveAccess(eq(cardId), any(UserPrincipal.class)))
                .thenReturn(cardResponse);

        mockMvc.perform(get("/api/v1/card/{cardId}", cardId)
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId.toString()));
    }


    @Test
    @DisplayName("Should return 404 when card not found")
    void getCardById_WhenCardNotFound_ShouldReturnNotFound() throws Exception {
        when(cardService.getCardByIdIfHaveAccess(eq(cardId), any()))
                .thenThrow(new NotFoundException("Card not found with id: " + cardId));

        mockMvc.perform(get("/api/v1/card/{cardId}", cardId)
                        .principal(() -> "testuser"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Card not found with id: " + cardId))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    @DisplayName("Should get all cards with pagination")
    void getAllCards_WhenCalled_ShouldReturnPagedResponse() throws Exception {
        when(cardService.getAllCards(any(Pageable.class), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull())).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/card")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(cardId.toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(cardService).getAllCards(any(Pageable.class), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("Should get all cards with filters")
    void getAllCards_WhenFiltersProvided_ShouldApplyFilters() throws Exception {
        when(cardService.getAllCards(any(Pageable.class), eq("test"), eq(CardStatus.ACTIVE),
                eq(new BigDecimal("100")), eq(new BigDecimal("1000")), any(), any()))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/card")
                        .param("page", "0")
                        .param("size", "10")
                        .param("search", "test")
                        .param("status", "ACTIVE")
                        .param("minBalance", "100")
                        .param("maxBalance", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(cardService).getAllCards(any(Pageable.class), eq("test"), eq(CardStatus.ACTIVE),
                eq(new BigDecimal("100")), eq(new BigDecimal("1000")), isNull(), isNull());
    }

    @Test
    @DisplayName("Should create card successfully")
    void createCard_WhenValidRequest_ShouldCreateCard() throws Exception {
        when(cardService.createCard(any(CreateCardRequest.class))).thenReturn(createCardResponse);

        mockMvc.perform(post("/api/v1/card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCardRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(cardId.toString()))
                .andExpect(jsonPath("$.cardNumber").value("4277011234567890"))
                .andExpect(jsonPath("$.holderName").value("JOHN DOE"))
                .andExpect(jsonPath("$.balance").value(500.00));

        verify(cardService).createCard(any(CreateCardRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when create card request is invalid")
    void createCard_WhenInvalidRequest_ShouldReturnBadRequest() throws Exception {
        CreateCardRequest invalidRequest = new CreateCardRequest();
        invalidRequest.setHolderName("A");
        invalidRequest.setInitialBalance(new BigDecimal("-100"));

        mockMvc.perform(post("/api/v1/card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        verify(cardService, never()).createCard(any());
    }

    @Test
    @DisplayName("Should block card successfully")
    void blockCard_WhenValidId_ShouldBlockCard() throws Exception {
        CardResponse blockedCard = CardResponse.builder()
                .id(cardId)
                .cardNumberMask("**** **** **** 1234")
                .status(CardStatus.BLOCKED)
                .build();

        when(cardService.blockCard(cardId)).thenReturn(blockedCard);

        mockMvc.perform(patch("/api/v1/card/{cardId}/block", cardId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(cardId.toString()))
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        verify(cardService).blockCard(cardId);
    }

    @Test
    @DisplayName("Should unblock card successfully")
    void unblockCard_WhenValidId_ShouldUnblockCard() throws Exception {
        when(cardService.unblockCard(cardId)).thenReturn(cardResponse);

        mockMvc.perform(patch("/api/v1/card/{cardId}/unblock", cardId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(cardId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(cardService).unblockCard(cardId);
    }

    @Test
    @DisplayName("Should activate card successfully")
    void activateCard_WhenValidId_ShouldActivateCard() throws Exception {
        when(cardService.activateCard(cardId)).thenReturn(cardResponse);

        mockMvc.perform(patch("/api/v1/card/{cardId}/activate", cardId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(cardId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(cardService).activateCard(cardId);
    }

    @Test
    @DisplayName("Should delete card successfully")
    void deleteCard_WhenValidId_ShouldDeleteCard() throws Exception {
        doNothing().when(cardService).deleteCard(cardId);

        mockMvc.perform(delete("/api/v1/card/{cardId}", cardId))
                .andExpect(status().isOk());

        verify(cardService).deleteCard(cardId);
    }

    @Test
    @DisplayName("Should handle card service exceptions")
    void blockCard_WhenServiceThrowsException_ShouldReturnError() throws Exception {
        when(cardService.blockCard(cardId))
                .thenThrow(new IllegalStateException("Cannot block expired card"));

        mockMvc.perform(patch("/api/v1/card/{cardId}/block", cardId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot block expired card"))
                .andExpect(jsonPath("$.statusCode").value(400));

        verify(cardService).blockCard(cardId);
    }

    @Test
    @DisplayName("Should handle date time parameters correctly")
    void getAllCards_WhenDateTimeFilters_ShouldParseCorrectly() throws Exception {
        String createdAfter = "2023-01-01T00:00:00";
        String createdBefore = "2023-12-31T23:59:59";

        when(cardService.getAllCards(any(Pageable.class), isNull(), isNull(),
                isNull(), isNull(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/card")
                        .param("createdAfter", createdAfter)
                        .param("createdBefore", createdBefore))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(cardService).getAllCards(any(Pageable.class), isNull(), isNull(),
                isNull(), isNull(), any(LocalDateTime.class), any(LocalDateTime.class));
    }
}
