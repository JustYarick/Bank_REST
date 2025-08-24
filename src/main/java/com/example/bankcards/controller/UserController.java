package com.example.bankcards.controller;

import com.example.bankcards.dto.core.PagedResponse;
import com.example.bankcards.dto.user.CreateUserRequest;
import com.example.bankcards.dto.user.UpdateUserRequest;
import com.example.bankcards.dto.user.UserResponse;
import com.example.bankcards.entity.UserRole;
import com.example.bankcards.service.interfaces.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Управление пользователями")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Получить всех пользователей", description = "Пагинация, поиск и фильтрация по роли, активности и дате создания")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<UserResponse>> getAllUsers(
            @Parameter(description = "Номер страницы (начиная с 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Поисковый запрос по username или email")
            @RequestParam(required = false) String search,
            @Parameter(description = "Фильтр по роли")
            @RequestParam(required = false) UserRole role,
            @Parameter(description = "Активные? true/false")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Создан после (ISO date-time)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @Parameter(description = "Создан до (ISO date-time)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore
    ) {
        return ResponseEntity.ok(userService.getAllUsers(
                page, size, search, role, active, createdAfter, createdBefore));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по ID", description = "Получение информации о пользователе по его ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "ID пользователя")
            @PathVariable UUID id) {

        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    @Operation(summary = "Создать пользователя", description = "Создание нового пользователя")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить пользователя", description = "Обновление данных пользователя")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "ID пользователя")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {

        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить пользователя", description = "Удаление пользователя")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID пользователя")
            @PathVariable UUID id) {

        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Активировать пользователя", description = "Активация учетной записи пользователя")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> activateUser(
            @Parameter(description = "ID пользователя")
            @PathVariable UUID id) {

        return ResponseEntity.ok(userService.activateUser(id));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Деактивировать пользователя", description = "Деактивация учетной записи пользователя")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> deactivateUser(
            @Parameter(description = "ID пользователя")
            @PathVariable UUID id) {

        return ResponseEntity.ok(userService.deactivateUser(id));
    }
}
