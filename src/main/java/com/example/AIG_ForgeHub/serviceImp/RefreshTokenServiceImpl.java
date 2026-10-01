package com.example.AIG_ForgeHub.serviceImp;

import com.example.AIG_ForgeHub.dto.LoginResponse;
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
    public LoginResponse refreshAccessToken(
            String refreshToken) {

        /*
         * STEP 1:
         * Check whether refresh token is structurally
         * valid and not expired.
         */
        if (!jwtService.isTokenValid(refreshToken)) {

            throw new RuntimeException(
                    "Invalid refresh token"
            );
        }

        /*
         * STEP 2:
         * Make sure this JWT is actually a
         * refresh token and not an access token.
         */
        if (!jwtService.isRefreshToken(refreshToken)) {

            throw new RuntimeException(
                    "Invalid refresh token"
            );
        }

        /*
         * STEP 3:
         * Find the user whose current refresh token
         * matches the incoming token.
         */
        User user =
                userRepository
                        .findByRefreshTokenHash(refreshToken)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Refresh token not found"
                                )
                        );

        /*
         * STEP 4:
         * Check database expiry.
         */
        if (user.getRefreshTokenExpiry() == null ||
                user.getRefreshTokenExpiry()
                        .isBefore(LocalDateTime.now())) {

            throw new RuntimeException(
                    "Refresh token expired"
            );
        }

        /*
         * STEP 5:
         * Determine whether the original refresh
         * token was a Remember Me token.
         *
         * Normal token = 1 day
         * Remember Me = 30 days
         */
        boolean rememberMe =
                user.getRefreshTokenExpiry()
                        .isAfter(
                                LocalDateTime.now()
                                        .plusDays(2)
                        );

        /*
         * STEP 6:
         * Generate NEW access token.
         */
        String newAccessToken =
                jwtService.generateAccessToken(
                        user.getEmail(),
                        user.getRole(),
                        true
                );

        /*
         * STEP 7:
         * IMPORTANT:
         * Generate NEW refresh token.
         *
         * Old refresh token will be replaced.
         */
        String newRefreshToken =
                generateRefreshToken(
                        user.getEmail(),
                        user.getRole(),
                        rememberMe
                );

        /*
         * At this point:
         *
         * OLD RT → invalid
         * NEW RT → stored in DB
         */

        return LoginResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .firstTimeLogin(
                        Boolean.TRUE.equals(
                                user.getIsFirstTimeLogin()
                        )
                )
                .build();
    }

    @Override
    public void deleteRefreshToken(
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElse(null);

        if (user != null) {

            /*
             * Revocation:
             *
             * Existing refresh token becomes
             * invalid immediately.
             */
            user.setRefreshTokenHash(null);

            user.setRefreshTokenExpiry(null);

            userRepository.save(user);
        }
    }
}