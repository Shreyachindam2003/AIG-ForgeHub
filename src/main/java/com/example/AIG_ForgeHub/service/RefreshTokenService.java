package com.example.AIG_ForgeHub.service;

import com.example.AIG_ForgeHub.dto.LoginResponse;

public interface RefreshTokenService {

    String generateRefreshToken(
            String email,
            String role,
            boolean rememberMe);

    LoginResponse refreshAccessToken(
            String refreshToken);

    void deleteRefreshToken(
            String email);
}