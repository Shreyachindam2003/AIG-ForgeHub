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

        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        User user =
                userRepository.findByEmail(
                        loginRequest.getEmail()
                ).orElseThrow(
                        () -> new RuntimeException("User not found")
                );

        /*
         * ADMIN ko Google Authenticator 2FA nahi chahiye.
         * Isliye ADMIN ka JWT directly mfaVerified=true hoga.
         *
         * VENDOR ke liye pehle password login hoga,
         * uske baad Google Authenticator verification hoga.
         */
        boolean mfaVerified =
                "ADMIN".equalsIgnoreCase(user.getRole());

        // Generate Access JWT
        String accessToken =
                jwtService.generateAccessToken(
                        user.getEmail(),
                        user.getRole(),
                        mfaVerified
                );

        // Generate Refresh JWT
        String refreshToken =
                refreshTokenService.generateRefreshToken(
                        user.getEmail(),
                        user.getRole(),
                        loginRequest.isRememberMe()
                );

        /*
         * First-time VENDOR ke liye Google Authenticator
         * secret key generate karenge.
         *
         * ADMIN ke liye secret key generate nahi hogi.
         */
        if ("VENDOR".equalsIgnoreCase(user.getRole())
                && Boolean.TRUE.equals(user.getIsFirstTimeLogin())
                && user.getSecretKey() == null) {

            String secretKey =
                    twoFactorService.generateSecretKey();

            user.setSecretKey(secretKey);

            userRepository.save(user);
        }

        return LoginResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .firstTimeLogin(
                        Boolean.TRUE.equals(
                                user.getIsFirstTimeLogin()
                        )
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
    public LoginResponse refreshAccessToken(
            String refreshToken) {

        return refreshTokenService
                .refreshAccessToken(refreshToken);
    }

    @Override
    public void logout(String email) {

        refreshTokenService.deleteRefreshToken(email);
    }
}
