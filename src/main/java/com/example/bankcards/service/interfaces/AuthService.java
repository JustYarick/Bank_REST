package com.example.bankcards.service.interfaces;

import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.auth.RegisterRequest;

public interface AuthService {

    AuthResponse login(String username, String password);

    AuthResponse register(RegisterRequest registerRequest);
}
