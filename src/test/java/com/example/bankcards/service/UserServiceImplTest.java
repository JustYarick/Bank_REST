package com.example.bankcards.service;

import com.example.bankcards.dto.core.PagedResponse;
import com.example.bankcards.dto.user.CreateUserRequest;
import com.example.bankcards.dto.user.UpdateUserRequest;
import com.example.bankcards.dto.user.UserResponse;
import com.example.bankcards.entity.CardEntity;
import com.example.bankcards.entity.UserEntity;
import com.example.bankcards.entity.UserRole;
import com.example.bankcards.exception.AlreadyTakenException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private UserEntity userEntity;
    private CreateUserRequest createRequest;
    private UpdateUserRequest updateRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        userEntity = UserEntity.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encoded-password")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .cards(new ArrayList<>())
                .build();

        createRequest = CreateUserRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .role(UserRole.USER)
                .build();

        updateRequest = new UpdateUserRequest();
        updateRequest.setUsername("updateduser");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("User");
        updateRequest.setRole(UserRole.ADMIN);
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void getUserById_WhenUserExists_ShouldReturnUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

        UserResponse result = userService.getUserById(userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFirstName()).isEqualTo("Test");
        assertThat(result.getLastName()).isEqualTo("User");
        assertThat(result.getRole()).isEqualTo(UserRole.USER);
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getCardsCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should throw NotFoundException when user not found")
    void getUserById_WhenUserNotFound_ShouldThrowNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found ID: " + userId);
    }

    @Test
    @DisplayName("Should create user successfully")
    void createUser_WhenValidRequest_ShouldCreateUser() {
        String encodedPassword = "encoded-password123";
        UserEntity savedUser = UserEntity.builder()
                .id(userId)
                .username(createRequest.getUsername())
                .email(createRequest.getEmail())
                .passwordHash(encodedPassword)
                .firstName(createRequest.getFirstName())
                .lastName(createRequest.getLastName())
                .role(createRequest.getRole())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .cards(new ArrayList<>())
                .build();

        when(userRepository.existsByUsername(createRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(createRequest.getPassword())).thenReturn(encodedPassword);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        UserResponse result = userService.createUser(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getUsername()).isEqualTo(createRequest.getUsername());
        assertThat(result.getEmail()).isEqualTo(createRequest.getEmail());
        assertThat(result.getFirstName()).isEqualTo(createRequest.getFirstName());
        assertThat(result.getLastName()).isEqualTo(createRequest.getLastName());
        assertThat(result.getRole()).isEqualTo(createRequest.getRole());
        assertThat(result.getIsActive()).isTrue();

        verify(userRepository).existsByUsername(createRequest.getUsername());
        verify(userRepository).existsByEmail(createRequest.getEmail());
        verify(passwordEncoder).encode(createRequest.getPassword());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should throw AlreadyTakenException when username exists")
    void createUser_WhenUsernameExists_ShouldThrowAlreadyTakenException() {
        when(userRepository.existsByUsername(createRequest.getUsername())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(createRequest))
                .isInstanceOf(AlreadyTakenException.class)
                .hasMessageContaining("Username is already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AlreadyTakenException when email exists")
    void createUser_WhenEmailExists_ShouldThrowAlreadyTakenException() {
        when(userRepository.existsByUsername(createRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(createRequest))
                .isInstanceOf(AlreadyTakenException.class)
                .hasMessageContaining("Email is already exist");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update user successfully")
    void updateUser_WhenValidRequest_ShouldUpdateUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userRepository.existsByUsername(updateRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(updateRequest.getEmail())).thenReturn(false);

        UserResponse result = userService.updateUser(userId, updateRequest);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(updateRequest.getUsername());
        assertThat(result.getEmail()).isEqualTo(updateRequest.getEmail());
        assertThat(result.getFirstName()).isEqualTo(updateRequest.getFirstName());
        assertThat(result.getLastName()).isEqualTo(updateRequest.getLastName());
        assertThat(result.getRole()).isEqualTo(updateRequest.getRole());

        assertThat(userEntity.getUsername()).isEqualTo(updateRequest.getUsername());
        assertThat(userEntity.getEmail()).isEqualTo(updateRequest.getEmail());
    }

    @Test
    @DisplayName("Should throw NotFoundException when updating non-existent user")
    void updateUser_WhenUserNotFound_ShouldThrowNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(userId, updateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found ID: " + userId);
    }

    @Test
    @DisplayName("Should throw AlreadyTakenException when updating to existing username")
    void updateUser_WhenUsernameAlreadyTaken_ShouldThrowAlreadyTakenException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userRepository.existsByUsername(updateRequest.getUsername())).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(userId, updateRequest))
                .isInstanceOf(AlreadyTakenException.class)
                .hasMessageContaining("Username is already taken");
    }

    @Test
    @DisplayName("Should update user with partial request")
    void updateUser_WhenPartialRequest_ShouldUpdateOnlyProvidedFields() {
        UpdateUserRequest partialRequest = new UpdateUserRequest();
        partialRequest.setFirstName("NewFirstName");

        String originalUsername = userEntity.getUsername();
        String originalEmail = userEntity.getEmail();

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

        UserResponse result = userService.updateUser(userId, partialRequest);

        assertThat(result.getFirstName()).isEqualTo("NewFirstName");
        assertThat(result.getUsername()).isEqualTo(originalUsername);
        assertThat(result.getEmail()).isEqualTo(originalEmail);
    }

    @Test
    @DisplayName("Should delete user successfully")
    void deleteUser_WhenUserExists_ShouldDeleteUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

        userService.deleteUser(userId);

        verify(userRepository).delete(userEntity);
    }

    @Test
    @DisplayName("Should throw NotFoundException when deleting non-existent user")
    void deleteUser_WhenUserNotFound_ShouldThrowNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found ID: " + userId);

        verify(userRepository, never()).delete((UserEntity) any());
    }

    @Test
    @DisplayName("Should activate user successfully")
    void activateUser_WhenUserExists_ShouldActivateUser() {
        userEntity.setIsActive(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

        UserResponse result = userService.activateUser(userId);

        assertThat(result.getIsActive()).isTrue();
        assertThat(userEntity.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should deactivate user successfully")
    void deactivateUser_WhenUserExists_ShouldDeactivateUser() {
        userEntity.setIsActive(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

        UserResponse result = userService.deactivateUser(userId);

        assertThat(result.getIsActive()).isFalse();
        assertThat(userEntity.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Should throw NotFoundException when activating non-existent user")
    void activateUser_WhenUserNotFound_ShouldThrowNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.activateUser(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found ID: " + userId);
    }

    @Test
    @DisplayName("Should get all users with pagination")
    void getAllUsers_WhenCalled_ShouldReturnPagedResponse() {
        List<UserEntity> users = List.of(userEntity);
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserEntity> page = new PageImpl<>(users, pageable, 1);

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PagedResponse<UserResponse> result = userService.getAllUsers(
                0, 10, null, null, null, null, null
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isTrue();
    }

    @Test
    @DisplayName("Should get all users with search filter")
    void getAllUsers_WhenSearchProvided_ShouldFilterUsers() {
        List<UserEntity> users = List.of(userEntity);
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserEntity> page = new PageImpl<>(users, pageable, 1);

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PagedResponse<UserResponse> result = userService.getAllUsers(
                0, 10, "test", UserRole.USER, true, null, null
        );

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Should handle empty user list")
    void getAllUsers_WhenNoUsers_ShouldReturnEmptyPage() {
        List<UserEntity> users = List.of();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserEntity> page = new PageImpl<>(users, pageable, 0);

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PagedResponse<UserResponse> result = userService.getAllUsers(
                0, 10, null, null, null, null, null
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should not allow updating to same username")
    void updateUser_WhenSameUsername_ShouldNotThrowException() {
        UpdateUserRequest sameUsernameRequest = new UpdateUserRequest();
        sameUsernameRequest.setUsername(userEntity.getUsername());

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

        assertThatCode(() -> userService.updateUser(userId, sameUsernameRequest))
                .doesNotThrowAnyException();

        verify(userRepository, never()).existsByUsername(any());
    }

    @Test
    @DisplayName("Should handle cards count correctly")
    void getUserById_WhenUserHasCards_ShouldReturnCorrectCardsCount() {
        userEntity.setCards(List.of(
                new CardEntity(), new CardEntity(), new CardEntity()
        ));

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

        UserResponse result = userService.getUserById(userId);

        assertThat(result.getCardsCount()).isEqualTo(3);
    }
}
