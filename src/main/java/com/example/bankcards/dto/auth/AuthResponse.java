package com.example.bankcards.dto.auth;

import com.example.bankcards.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;

    public AuthResponse(String token, UUID id, String username, String email,
                        String firstName, String lastName, UserRole role) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }
}