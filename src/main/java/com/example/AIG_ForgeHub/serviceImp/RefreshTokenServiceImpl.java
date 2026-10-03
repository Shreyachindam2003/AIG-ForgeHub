package com.example.AIG_ForgeHub.serviceImp;

import com.example.AIG_ForgeHub.dto.LoginResponse;
import com.example.AIG_ForgeHub.model.User;
import com.example.AIG_ForgeHub.repository.UserRepository;
import com.example.AIG_ForgeHub.security.JwtService;
import com.example.AIG_ForgeHub.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${jwt.remember-me-expiration-ms}")
    private long rememberMeExpirationMs;

    @Override
    public String generateRefreshToken(String email,String role,boolean rememberMe) {

        long expiration=rememberMe
                ? rememberMeExpirationMs
                : refreshTokenExpirationMs;

        String token=jwtService.generateRefreshToken(
                email,
                role,
                expiration
        );

        User user=userRepository.findByEmail(email)
                .orElseThrow(()->new RuntimeException("User not found"));

        user.setRefreshTokenHash(token);
        user.setRefreshTokenExpiry(
                LocalDateTime.now().plusNanos(expiration*1_000_000)
        );

        userRepository.save(user);

        log.info("Refresh token generated and stored for user: {} with rememberMe: {}",email,rememberMe);

        return token;
    }

    @Override
    public LoginResponse refreshAccessToken(String refreshToken) {

        if (refreshToken==null || refreshToken.isBlank()) {

            log.warn("Refresh token missing");

            throw new RuntimeException("Refresh token missing");
        }

        if (!jwtService.isTokenValid(refreshToken)) {

            log.warn("Invalid refresh token received");

            throw new RuntimeException("Invalid refresh token");
        }

        if (!jwtService.isRefreshToken(refreshToken)) {

            log.warn("Non-refresh token used for token refresh");

            throw new RuntimeException("Invalid refresh token");
        }

        User user=userRepository.findByRefreshTokenHash(refreshToken)
                .orElseThrow(()->{
                    log.warn("Refresh token not found in database");
                    return new RuntimeException("Refresh token not found");
                });

        if (user.getRefreshTokenExpiry()==null ||
                user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {

            log.warn("Refresh token expired for user: {}",user.getEmail());

            throw new RuntimeException("Refresh token expired");
        }

        String newAccessToken=jwtService.generateAccessToken(
                user.getEmail(),
                user.getRole(),
                true
        );

        log.info("New access token generated using refresh token for user: {}",user.getEmail());

        return LoginResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .firstTimeLogin(Boolean.TRUE.equals(user.getIsFirstTimeLogin()))
                .build();
    }

    @Override
    public void deleteRefreshToken(String refreshToken) {

        if (refreshToken==null || refreshToken.isBlank()) {

            log.warn("Logout requested without refresh token");

            return;
        }

        User user=userRepository.findByRefreshTokenHash(refreshToken)
                .orElse(null);

        if (user!=null) {

            user.setRefreshTokenHash(null);
            user.setRefreshTokenExpiry(null);
            userRepository.save(user);

            log.info("Refresh token revoked successfully for user: {}",user.getEmail());

        } else {

            log.warn("Refresh token not found during logout");
        }
    }
}