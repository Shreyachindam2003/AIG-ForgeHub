package com.example.AIG_ForgeHub.serviceImp;


import com.example.AIG_ForgeHub.model.User;
import com.example.AIG_ForgeHub.repository.UserRepository;
import com.example.AIG_ForgeHub.security.JwtService;
import com.example.AIG_ForgeHub.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl
        implements RefreshTokenService {

    private final UserRepository userRepository;

    private final JwtService jwtService;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${jwt.remember-me-expiration-ms}")
    private long rememberMeExpirationMs;

    @Override
    public String generateRefreshToken(
            String email,
            String role,
            boolean rememberMe) {

        long expiration =
                rememberMe
                        ? rememberMeExpirationMs
                        : refreshTokenExpirationMs;

        String token =
                jwtService.generateRefreshToken(
                        email,
                        role,
                        expiration
                );

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "User not found"
                                )
                        );

        user.setRefreshTokenHash(token);
        user.setRefreshTokenExpiry(
                LocalDateTime.now()
                        .plusNanos(
                                expiration * 1_000_000
                        )
        );

        userRepository.save(user);

        return token;
    }

    @Override
    public String refreshAccessToken(String refreshToken) {

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        User user =
                userRepository
                        .findByRefreshTokenHash(refreshToken)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Refresh token not found"
                                )
                        );

        if (user.getRefreshTokenExpiry() == null ||
                user.getRefreshTokenExpiry()
                        .isBefore(LocalDateTime.now())) {

            throw new RuntimeException(
                    "Refresh token expired"
            );
        }

        return jwtService.generateAccessToken(
                user.getEmail(),
                user.getRole(),
                true
        );
    }

    @Override
    public void deleteRefreshToken(String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElse(null);

        if (user != null) {

            user.setRefreshTokenHash(null);            user.setRefreshTokenExpiry(null);

            userRepository.save(user);
        }
    }
}
