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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements com.example.bankcards.service.interfaces.UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse getUserById(UUID id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found ID: " + id));
        return UserResponse.convert(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        ensureUnique(request.getUsername(), request.getEmail());

        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(request.getRole());
        user.setIsActive(true);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        UserEntity saved = userRepository.save(user);
        return UserResponse.convert(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found ID: " + id));

        validateUniqueness(request, user);

        if (request.getUsername() != null) user.setUsername(request.getUsername());
        if (request.getEmail()    != null) user.setEmail(request.getEmail());
        if (request.getFirstName()!= null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getRole()     != null) user.setRole(request.getRole());
        user.setUpdatedAt(LocalDateTime.now());

        return UserResponse.convert(user);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found ID: " + id));
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public UserResponse activateUser(UUID id) {
        return setActiveStatus(id, true);
    }

    @Override
    @Transactional
    public UserResponse deactivateUser(UUID id) {
        return setActiveStatus(id, false);
    }

    private UserResponse setActiveStatus(UUID id, boolean active) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found ID: " + id));
        user.setIsActive(active);
        user.setUpdatedAt(LocalDateTime.now());
        return UserResponse.convert(user);
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

        return PagedResponse.<UserResponse>builder()
                .content(usersPage.map(UserResponse::convert).getContent())
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

    private void ensureUnique(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new AlreadyTakenException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new AlreadyTakenException("Email is already exist");
        }
    }

    private void validateUniqueness(UpdateUserRequest req, UserEntity existing) {
        if (req.getUsername() != null
                && !req.getUsername().equals(existing.getUsername())
                && userRepository.existsByUsername(req.getUsername())) {
            throw new AlreadyTakenException("Username is already taken");
        }
        if (req.getEmail() != null
                && !req.getEmail().equals(existing.getEmail())
                && userRepository.existsByEmail(req.getEmail())) {
            throw new AlreadyTakenException("Email is already exist");
        }
    }
}
