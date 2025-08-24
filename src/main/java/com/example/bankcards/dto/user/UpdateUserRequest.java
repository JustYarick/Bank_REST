package com.example.bankcards.dto.user;

import com.example.bankcards.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Size(max = 50, message = "Username не может превышать 50 символов")
    private String username;

    @Email(message = "Некорректный email")
    @Size(max = 100, message = "Email не может превышать 100 символов")
    private String email;

    @Size(max = 50, message = "Имя не может превышать 50 символов")
    private String firstName;

    @Size(max = 50, message = "Фамилия не может превышать 50 символов")
    private String lastName;

    private UserRole role;
}