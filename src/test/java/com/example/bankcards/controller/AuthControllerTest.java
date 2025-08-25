package com.example.bankcards.controller;

import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.auth.LoginRequest;
import com.example.bankcards.dto.auth.RegisterRequest;
import com.example.bankcards.entity.UserRole;
import com.example.bankcards.exception.AlreadyTakenException;
import com.example.bankcards.exception.DefaultExceptionHandler;
import com.example.bankcards.service.interfaces.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(authService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new DefaultExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();

        loginRequest = new LoginRequest("testuser", "password123");

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");

        authResponse = new AuthResponse(
                "jwt-token-123",
                UUID.randomUUID(),
                "testuser",
                "test@example.com",
                "Test",
                "User",
                UserRole.USER
        );
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void login_WhenValidCredentials_ShouldReturnAuthResponse() throws Exception {
        when(authService.login("testuser", "password123")).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(authService).login("testuser", "password123");
    }

    @Test
    @DisplayName("Should return 400 when username is too short")
    void login_WhenUsernameTooShort_ShouldReturnBadRequest() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("ab", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.statusCode").value(400));

        verify(authService, never()).login(any(), any());
    }

    @Test
    @DisplayName("Should return 400 when password is too short")
    void login_WhenPasswordTooShort_ShouldReturnBadRequest() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("testuser", "12345");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        verify(authService, never()).login(any(), any());
    }

    @Test
    @DisplayName("Should register successfully with valid data")
    void register_WhenValidData_ShouldReturnAuthResponse() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when register request has invalid email")
    void register_WhenInvalidEmail_ShouldReturnBadRequest() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest();
        invalidRequest.setUsername("testuser");
        invalidRequest.setEmail("invalid-email");
        invalidRequest.setPassword("password123");
        invalidRequest.setFirstName("Test");
        invalidRequest.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("Should return 409 when username already exists")
    void register_WhenUsernameExists_ShouldReturnConflict() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new AlreadyTakenException("Username is already taken"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username is already taken"))
                .andExpect(jsonPath("$.statusCode").value(409));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should handle authentication failure")
    void login_WhenAuthenticationFails_ShouldReturnBadRequest() throws Exception {
        when(authService.login("testuser", "wrongpassword"))
                .thenThrow(new IllegalArgumentException("Invalid credentials"));

        LoginRequest invalidLogin = new LoginRequest("testuser", "wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        verify(authService).login("testuser", "wrongpassword");
    }
}
