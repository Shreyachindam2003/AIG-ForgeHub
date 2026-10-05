package com.example.AIG_ForgeHub.serviceImpl;

import com.example.AIG_ForgeHub.dto.authDto.LoginRequest;
import com.example.AIG_ForgeHub.dto.authDto.LoginResponse;
import com.example.AIG_ForgeHub.entity.User;
import com.example.AIG_ForgeHub.repository.UserRepository;
import com.example.AIG_ForgeHub.security.JwtService;
import com.example.AIG_ForgeHub.service.AuthService;
import com.example.AIG_ForgeHub.service.RefreshTokenService;
import com.example.AIG_ForgeHub.service.TwoFactorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TwoFactorService twoFactorService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {

        log.info("Authenticating user: {}",loginRequest.getEmail());

        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        log.info("Password authentication successful for user: {}",loginRequest.getEmail());

        User user=userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(()->new RuntimeException("User not found"));

        boolean firstTimeLogin=Boolean.TRUE.equals(user.getIsFirstTimeLogin());

        if(user.getSecretKey()==null || user.getSecretKey().isBlank()) {

            String secretKey=twoFactorService.generateSecretKey();

            user.setSecretKey(secretKey);

            userRepository.save(user);

            log.info("2FA secret generated for user: {}",user.getEmail());
        }

        log.info("MFA verification required for user: {}",user.getEmail());

        return LoginResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(null)
                .refreshToken(null)
                .firstTimeLogin(firstTimeLogin)
                .build();
    }

    @Override
    public LoginResponse completeTwoFactor(String email,boolean rememberMe) {

        log.info("Completing 2FA authentication for user: {}",email);

        User user=userRepository.findByEmail(email)
                .orElseThrow(()->new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        String refreshToken=refreshTokenService.generateRefreshToken(
                user.getEmail(),
                user.getRole(),
                rememberMe
        );

        String accessToken=jwtService.generateAccessToken(
                user.getEmail(),
                user.getRole(),
                true
        );

        user.setIsFirstTimeLogin(false);

        userRepository.save(user);

        log.info("2FA authentication completed successfully for user: {}",email);

        return LoginResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .firstTimeLogin(false)
                .build();
    }

    @Override
    public LoginResponse refreshAccessToken(String refreshToken) {

        log.info("Access token refresh requested");

        LoginResponse response=refreshTokenService.refreshAccessToken(refreshToken);

        log.info("Access token refreshed successfully for user: {}",response.getEmail());

        return response;
    }

    @Override
    public void logout(String refreshToken) {

        refreshTokenService.deleteRefreshToken(refreshToken);

        log.info("Refresh token revoked successfully");
    }
}