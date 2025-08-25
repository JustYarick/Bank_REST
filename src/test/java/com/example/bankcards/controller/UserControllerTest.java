package com.example.bankcards.controller;

import com.example.bankcards.dto.core.PagedResponse;
import com.example.bankcards.dto.user.CreateUserRequest;
import com.example.bankcards.dto.user.UpdateUserRequest;
import com.example.bankcards.dto.user.UserResponse;
import com.example.bankcards.entity.UserRole;
import com.example.bankcards.exception.AlreadyTakenException;
import com.example.bankcards.exception.DefaultExceptionHandler;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.service.interfaces.UserService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID userId;
    private UserResponse userResponse;
    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;
    private PagedResponse<UserResponse> pagedResponse;

    @BeforeEach
    void setUp() {
        UserController userController = new UserController(userService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new DefaultExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();

        userId = UUID.randomUUID();

        userResponse = UserResponse.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .cardsCount(2)
                .build();

        createUserRequest = CreateUserRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .role(UserRole.USER)
                .build();

        updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setUsername("updateduser");
        updateUserRequest.setEmail("updated@example.com");
        updateUserRequest.setFirstName("Updated");
        updateUserRequest.setLastName("User");
        updateUserRequest.setRole(UserRole.ADMIN);

        pagedResponse = PagedResponse.<UserResponse>builder()
                .content(List.of(userResponse))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();
    }

    @Test
    @DisplayName("Should get all users with pagination")
    void getAllUsers_WhenCalled_ShouldReturnPagedResponse() throws Exception {
        when(userService.getAllUsers(0, 10, null, null, null, null, null))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(userId.toString()))
                .andExpect(jsonPath("$.content[0].username").value("testuser"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(userService).getAllUsers(0, 10, null, null, null, null, null);
    }

    @Test
    @DisplayName("Should get all users with filters")
    void getAllUsers_WhenFiltersProvided_ShouldApplyFilters() throws Exception {
        when(userService.getAllUsers(0, 10, "test", UserRole.USER, true, null, null))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "10")
                        .param("search", "test")
                        .param("role", "USER")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(userService).getAllUsers(0, 10, "test", UserRole.USER, true, null, null);
    }

    @Test
    @DisplayName("Should handle date time parameters correctly")
    void getAllUsers_WhenDateTimeFilters_ShouldParseCorrectly() throws Exception {
        String createdAfter = "2023-01-01T00:00:00";
        String createdBefore = "2023-12-31T23:59:59";

        when(userService.getAllUsers(eq(0), eq(10), isNull(), isNull(), isNull(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/users")
                        .param("createdAfter", createdAfter)
                        .param("createdBefore", createdBefore))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(userService).getAllUsers(eq(0), eq(10), isNull(), isNull(), isNull(),
                any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void getUserById_WhenValidId_ShouldReturnUser() throws Exception {
        when(userService.getUserById(userId)).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.cardsCount").value(2));

        verify(userService).getUserById(userId);
    }

    @Test
    @DisplayName("Should return 404 when user not found")
    void getUserById_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
        when(userService.getUserById(userId))
                .thenThrow(new NotFoundException("User not found with id: " + userId));

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: " + userId))
                .andExpect(jsonPath("$.statusCode").value(404));

        verify(userService).getUserById(userId);
    }

    @Test
    @DisplayName("Should create user successfully")
    void createUser_WhenValidRequest_ShouldCreateUser() throws Exception {
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService).createUser(any(CreateUserRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when create user request is invalid - blank username")
    void createUser_WhenBlankUsername_ShouldReturnBadRequest() throws Exception {
        CreateUserRequest invalidRequest = CreateUserRequest.builder()
                .username("")
                .email("test@example.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("Should return 400 when create user request has invalid email")
    void createUser_WhenInvalidEmail_ShouldReturnBadRequest() throws Exception {
        CreateUserRequest invalidRequest = CreateUserRequest.builder()
                .username("testuser")
                .email("invalid-email")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("Should return 400 when password is too short")
    void createUser_WhenPasswordTooShort_ShouldReturnBadRequest() throws Exception {
        CreateUserRequest invalidRequest = CreateUserRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("123")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("Should return 409 when username already exists")
    void createUser_WhenUsernameExists_ShouldReturnConflict() throws Exception {
        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new AlreadyTakenException("Username is already taken"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username is already taken"))
                .andExpect(jsonPath("$.statusCode").value(409));

        verify(userService).createUser(any(CreateUserRequest.class));
    }

    @Test
    @DisplayName("Should update user successfully")
    void updateUser_WhenValidRequest_ShouldUpdateUser() throws Exception {
        when(userService.updateUser(eq(userId), any(UpdateUserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId.toString()));

        verify(userService).updateUser(eq(userId), any(UpdateUserRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when update user request has invalid email")
    void updateUser_WhenInvalidEmail_ShouldReturnBadRequest() throws Exception {
        UpdateUserRequest invalidRequest = new UpdateUserRequest();
        invalidRequest.setEmail("invalid-email");

        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent user")
    void updateUser_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
        when(userService.updateUser(eq(userId), any(UpdateUserRequest.class)))
                .thenThrow(new NotFoundException("User not found with id: " + userId));

        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: " + userId));

        verify(userService).updateUser(eq(userId), any(UpdateUserRequest.class));
    }

    @Test
    @DisplayName("Should delete user successfully")
    void deleteUser_WhenValidId_ShouldDeleteUser() throws Exception {
        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/api/v1/users/{id}", userId))
                .andExpect(status().isOk());

        verify(userService).deleteUser(userId);
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent user")
    void deleteUser_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
        doThrow(new NotFoundException("User not found with id: " + userId))
                .when(userService).deleteUser(userId);

        mockMvc.perform(delete("/api/v1/users/{id}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: " + userId));

        verify(userService).deleteUser(userId);
    }

    @Test
    @DisplayName("Should activate user successfully")
    void activateUser_WhenValidId_ShouldActivateUser() throws Exception {
        UserResponse activeUserResponse = UserResponse.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .isActive(true)
                .build();

        when(userService.activateUser(userId)).thenReturn(activeUserResponse);

        mockMvc.perform(patch("/api/v1/users/{id}/activate", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.isActive").value(true));

        verify(userService).activateUser(userId);
    }

    @Test
    @DisplayName("Should deactivate user successfully")
    void deactivateUser_WhenValidId_ShouldDeactivateUser() throws Exception {
        UserResponse inactiveUserResponse = UserResponse.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .isActive(false)
                .build();

        when(userService.deactivateUser(userId)).thenReturn(inactiveUserResponse);

        mockMvc.perform(patch("/api/v1/users/{id}/deactivate", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.isActive").value(false));

        verify(userService).deactivateUser(userId);
    }

    @Test
    @DisplayName("Should handle invalid UUID format")
    void getUserById_WhenInvalidUUID_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", "invalid-uuid"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getUserById(any());
    }

    @Test
    @DisplayName("Should handle missing content type")
    void createUser_WhenMissingContentType_ShouldReturnUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isUnsupportedMediaType());

        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("Should handle service exceptions")
    void getUserById_WhenServiceThrowsException_ShouldReturnError() throws Exception {
        when(userService.getUserById(userId))
                .thenThrow(new IllegalStateException("Database connection failed"));

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Database connection failed"))
                .andExpect(jsonPath("$.statusCode").value(400));

        verify(userService).getUserById(userId);
    }
}
