package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
    private final String username;
    private final String[] roles;
}
