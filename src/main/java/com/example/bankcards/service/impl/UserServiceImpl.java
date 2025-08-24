package com.example.bankcards.service.impl;

import com.example.bankcards.dto.core.PagedResponse;
import com.example.bankcards.dto.user.CreateUserRequest;
import com.example.bankcards.dto.user.UpdateUserRequest;
import com.example.bankcards.dto.user.UserResponse;
import com.example.bankcards.entity.UserEntity;
import com.example.bankcards.entity.UserRole;
import com.example.bankcards.exception.AlreadyTakenException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.interfaces.UserService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse getUserById(UUID id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден с ID: " + id));
        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AlreadyTakenException("Пользователь с таким username уже существует");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AlreadyTakenException("Пользователь с таким email уже существует");
        }

        UserEntity user = UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UserEntity saved = userRepository.save(user);
        return mapToUserResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден с ID: " + id));

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())
                && userRepository.existsByUsername(request.getUsername())) {
            throw new AlreadyTakenException("Пользователь с таким username уже существует");
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new AlreadyTakenException("Пользователь с таким email уже существует");
        }

        user = UserEntity.builder()
                .id(user.getId())
                .username(request.getUsername() != null ? request.getUsername() : user.getUsername())
                .email(request.getEmail() != null ? request.getEmail() : user.getEmail())
                .passwordHash(user.getPasswordHash())
                .firstName(request.getFirstName() != null ? request.getFirstName() : user.getFirstName())
                .lastName(request.getLastName() != null ? request.getLastName() : user.getLastName())
                .role(request.getRole() != null ? request.getRole() : user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .cards(user.getCards())
                .createdTransactions(user.getCreatedTransactions())
                .build();

        UserEntity updated = userRepository.save(user);
        return mapToUserResponse(updated);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден с ID: " + id));
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public UserResponse activateUser(UUID id) {
        return toggleUserStatus(id, true);
    }

    @Override
    @Transactional
    public UserResponse deactivateUser(UUID id) {
        return toggleUserStatus(id, false);
    }

    private UserResponse toggleUserStatus(UUID id, boolean isActive) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден с ID: " + id));

        user = UserEntity.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .isActive(isActive)
                .createdAt(user.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .cards(user.getCards())
                .createdTransactions(user.getCreatedTransactions())
                .build();

        UserEntity updated = userRepository.save(user);
        return mapToUserResponse(updated);
    }

    @Override
    public PagedResponse<UserResponse> getAllUsers(
            int page,
            int size,
            String search,
            UserRole role,
            Boolean active,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<UserEntity> spec = buildSpecification(search, role, active, createdAfter, createdBefore);
        Page<UserEntity> usersPage = userRepository.findAll(spec, pageable);

        List<UserResponse> content = usersPage.getContent().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());

        return PagedResponse.<UserResponse>builder()
                .content(content)
                .page(usersPage.getNumber())
                .size(usersPage.getSize())
                .totalElements(usersPage.getTotalElements())
                .totalPages(usersPage.getTotalPages())
                .first(usersPage.isFirst())
                .last(usersPage.isLast())
                .build();
    }

    private Specification<UserEntity> buildSpecification(
            String search,
            UserRole role,
            Boolean active,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), like),
                        cb.like(cb.lower(root.get("email")), like)
                ));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("isActive"), active));
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

    private UserResponse mapToUserResponse(UserEntity user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .cardsCount(user.getCards() != null ? user.getCards().size() : 0)
                .build();
    }
}
