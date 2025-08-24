package com.example.bankcards.service.interfaces;

import com.example.bankcards.dto.core.PagedResponse;
import com.example.bankcards.dto.user.CreateUserRequest;
import com.example.bankcards.dto.user.UpdateUserRequest;
import com.example.bankcards.dto.user.UserResponse;
import com.example.bankcards.entity.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public interface UserService {
    UserResponse getUserById(UUID id);
    UserResponse createUser(CreateUserRequest request);
    UserResponse updateUser(UUID id, UpdateUserRequest request);
    void deleteUser(UUID id);
    UserResponse activateUser(UUID id);
    UserResponse deactivateUser(UUID id);
    PagedResponse<UserResponse> getAllUsers(
            int page,
            int size,
            String search,
            UserRole role,
            Boolean active,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore
    );
}
