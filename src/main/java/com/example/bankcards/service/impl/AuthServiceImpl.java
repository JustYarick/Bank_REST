package com.example.bankcards.service.impl;

import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.auth.RegisterRequest;
import com.example.bankcards.entity.UserEntity;
import com.example.bankcards.entity.UserRole;
import com.example.bankcards.exception.AlreadyTakenException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.interfaces.AuthService;
import com.example.bankcards.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthResponse login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        username,
                        password
        ));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtils.generateJwtToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return new AuthResponse(
                jwt,
                userPrincipal.getId(),
                userPrincipal.getUsername(),
                userPrincipal.getEmail(),
                userRepository.findById(userPrincipal.getId()).get().getFirstName(),
                userRepository.findById(userPrincipal.getId()).get().getLastName(),
                UserRole.valueOf(userPrincipal.getAuthorities().iterator().next()
                        .getAuthority().replace("ROLE_", ""))
        );
    }

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new AlreadyTakenException("Username is already taken");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new AlreadyTakenException("Email is already in use");
        }

        new UserEntity();
        UserEntity user = UserEntity.builder()
            .username(registerRequest.getUsername())
            .email(registerRequest.getEmail())
            .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
            .firstName(registerRequest.getFirstName())
            .lastName(registerRequest.getLastName())
            .role(UserRole.USER)
            .isActive(true)
            .build();

        userRepository.save(user);

        log.info("New user registered: {}", user.getUsername());

        return login(registerRequest.getUsername(), registerRequest.getPassword());
    }
}
