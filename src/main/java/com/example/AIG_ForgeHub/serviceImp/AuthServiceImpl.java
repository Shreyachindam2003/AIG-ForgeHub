package com.example.AIG_ForgeHub.serviceImp;


import com.example.AIG_ForgeHub.dto.LoginRequest;
import com.example.AIG_ForgeHub.dto.LoginResponse;
import com.example.AIG_ForgeHub.model.User;
import com.example.AIG_ForgeHub.repository.UserRepository;
import com.example.AIG_ForgeHub.security.JwtService;
import com.example.AIG_ForgeHub.service.AuthService;
import com.example.AIG_ForgeHub.service.RefreshTokenService;
import com.example.AIG_ForgeHub.service.TwoFactorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final TwoFactorService twoFactorService;

    private final RefreshTokenService refreshTokenService;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {

        try {

            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

        } catch (AuthenticationException e) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        User user = userRepository
                .findByEmail(loginRequest.getEmail())
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        /*
         * First login:
         * SecretKey abhi nahi hai.
         */
        if (Boolean.TRUE.equals(user.getIsFirstTimeLogin())
                && user.getSecretKey() == null) {

            String secretKey =
                    twoFactorService.generateSecretKey();

            user.setSecretKey(secretKey);

            userRepository.save(user);
        }

        /*
         * Temporary access token.
         * MFA complete hone tak dashboard access nahi milega.
         */
        String accessToken =
                jwtService.generateAccessToken(
                        user.getEmail(),
                        user.getRole(),
                        false
                );

        String refreshToken =
                refreshTokenService.generateRefreshToken(
                        user.getEmail(),
                        user.getRole(),
                        loginRequest.isRememberMe()
                );

        return LoginResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .firstTimeLogin(
                        Boolean.TRUE.equals(user.getIsFirstTimeLogin())
                )
                .build();
    }

    @Override
    public LoginResponse completeTwoFactor(
            String email,
            boolean rememberMe) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        String accessToken =
                jwtService.generateAccessToken(
                        user.getEmail(),
                        user.getRole(),
                        true
                );

        String refreshToken =
                refreshTokenService.generateRefreshToken(
                        user.getEmail(),
                        user.getRole(),
                        rememberMe
                );

        user.setIsFirstTimeLogin(false);

        userRepository.save(user);

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
    public void logout(String email) {

        refreshTokenService.deleteRefreshToken(email);
    }
}
