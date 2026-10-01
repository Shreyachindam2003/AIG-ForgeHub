package com.example.AIG_ForgeHub.service;

public interface RefreshTokenService {

    String generateRefreshToken(
            String email,
            String role,
            boolean rememberMe);

    String refreshAccessToken(String refreshToken);

    void deleteRefreshToken(String email);
}