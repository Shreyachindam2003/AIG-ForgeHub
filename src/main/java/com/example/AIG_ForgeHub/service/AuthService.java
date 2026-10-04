package com.example.AIG_ForgeHub.service;

import com.example.AIG_ForgeHub.dto.LoginRequest;
import com.example.AIG_ForgeHub.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest loginRequest);

    LoginResponse completeTwoFactor(String email,boolean rememberMe);

    LoginResponse refreshAccessToken(String refreshToken);

    void logout(String refreshToken);
}