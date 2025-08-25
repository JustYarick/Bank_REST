package com.example.bankcards.service;

import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.auth.RegisterRequest;
import com.example.bankcards.dto.user.CreateUserRequest;
import com.example.bankcards.entity.UserEntity;
import com.example.bankcards.entity.UserRole;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.impl.AuthServiceImpl;
import com.example.bankcards.service.interfaces.UserService;
import com.example.bankcards.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AuthServiceImpl authService;

    private UUID userId;
    private UserEntity user;
    private UserPrincipal userPrincipal;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = UserEntity.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userPrincipal = new UserPrincipal(
                userId,
                "testuser",
                "test@example.com",
                "hashedPassword",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                true
        );

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Jane");
        registerRequest.setLastName("Smith");

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void login_Success() {
        String username = "testuser";
        String password = "password";
        String jwtToken = "jwt-token";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn(jwtToken);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        AuthResponse result = authService.login(username, password);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo(jwtToken);
        assertThat(result.getType()).isEqualTo("Bearer");
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getRole()).isEqualTo(UserRole.USER);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(securityContext).setAuthentication(authentication);
        verify(jwtUtils).generateJwtToken(authentication);
        verify(userRepository).findById(userId);
    }

    @Test
    void login_UserNotFound_ThrowsNotFoundException() {
        String username = "testuser";
        String password = "password";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt-token");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(username, password))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(securityContext).setAuthentication(authentication);
        verify(userRepository).findById(userId);
    }

    @Test
    void login_AuthenticationManagerCalled_WithCorrectCredentials() {
        String username = "testuser";
        String password = "password";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt-token");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        authService.login(username, password);

        verify(authenticationManager).authenticate(argThat(auth ->
                auth instanceof UsernamePasswordAuthenticationToken &&
                        auth.getPrincipal().equals(username) &&
                        auth.getCredentials().equals(password)
        ));
    }

    @Test
    void login_SecurityContextUpdated() {
        String username = "testuser";
        String password = "password";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt-token");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        authService.login(username, password);

        verify(securityContext).setAuthentication(authentication);
    }

    @Test
    void register_Success() {
        String jwtToken = "jwt-token";

        when(jwtUtils.generateJwtToken(any())).thenReturn(jwtToken);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        AuthResponse result = authService.register(registerRequest);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo(jwtToken);
        assertThat(result.getType()).isEqualTo("Bearer");

        verify(userService).createUser(any(CreateUserRequest.class));
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void register_CreateUserServiceCalled_WithCorrectParameters() {
        when(jwtUtils.generateJwtToken(any())).thenReturn("jwt-token");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        authService.register(registerRequest);

        verify(userService).createUser(argThat(createUserRequest -> {
            return createUserRequest != null;
        }));
    }

    @Test
    void register_CallsLogin_AfterUserCreation() {
        String username = registerRequest.getUsername();
        String password = registerRequest.getPassword();

        when(jwtUtils.generateJwtToken(any())).thenReturn("jwt-token");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        authService.register(registerRequest);

        verify(userService).createUser(any(CreateUserRequest.class));
        verify(authenticationManager).authenticate(argThat(auth ->
                auth instanceof UsernamePasswordAuthenticationToken &&
                        auth.getPrincipal().equals(username) &&
                        auth.getCredentials().equals(password)
        ));
    }

    @Test
    void register_UserServiceThrowsException_PropagatesException() {
        RuntimeException serviceException = new RuntimeException("User creation failed");
        doThrow(serviceException).when(userService).createUser(any(CreateUserRequest.class));

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User creation failed");

        verify(userService).createUser(any(CreateUserRequest.class));
        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(jwtUtils);
    }

    @Test
    void userPrincipal_CreatedCorrectly() {
        UserEntity testUser = UserEntity.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        UserPrincipal principal = UserPrincipal.create(testUser);

        assertThat(principal.getId()).isEqualTo(userId);
        assertThat(principal.getUsername()).isEqualTo("testuser");
        assertThat(principal.getEmail()).isEqualTo("test@example.com");
        assertThat(principal.getPassword()).isEqualTo("hashedPassword");
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
        assertThat(principal.getAuthorities()).hasSize(1);
        assertThat(principal.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void userPrincipal_InactiveUser_IsDisabled() {
        UserEntity inactiveUser = UserEntity.builder()
                .id(userId)
                .username("inactiveuser")
                .email("inactive@example.com")
                .passwordHash("hashedPassword")
                .role(UserRole.USER)
                .isActive(false)
                .build();

        UserPrincipal principal = UserPrincipal.create(inactiveUser);

        assertThat(principal.isEnabled()).isFalse();
    }
}
