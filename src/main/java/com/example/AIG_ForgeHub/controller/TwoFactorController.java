package com.example.AIG_ForgeHub.controller;

import com.example.AIG_ForgeHub.dto.LoginResponse;
import com.example.AIG_ForgeHub.dto.VerifyOtpRequest;
import com.example.AIG_ForgeHub.model.User;
import com.example.AIG_ForgeHub.repository.UserRepository;
import com.example.AIG_ForgeHub.security.JwtService;
import com.example.AIG_ForgeHub.service.AuthService;
import com.example.AIG_ForgeHub.service.TwoFactorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private final JwtService jwtService;

    private final UserRepository userRepository;

    private final TwoFactorService twoFactorService;

    private final AuthService authService;


    @GetMapping("/qr")
    public String qrPage(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader,
            Model model) {

        String token =
                getTokenFromHeader(authorizationHeader);

        if (token == null) {
            return "redirect:/auth/login";
        }

        try {

            String email =
                    jwtService.extractSubject(token);

            String qrCode =
                    twoFactorService.generateQrCode(email);

            model.addAttribute(
                    "qrCode",
                    qrCode
            );

            return "show-qr";

        } catch (Exception e) {

            e.printStackTrace();

            return "redirect:/auth/login";
        }
    }


    @GetMapping("/verify")
    public String verifyPage() {

        return "verify-otp";
    }


    @PostMapping("/verify")
    @ResponseBody
    public Map<String, Object> verifyOtp(
            @Valid @ModelAttribute VerifyOtpRequest request,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader) {

        String token =
                getTokenFromHeader(authorizationHeader);

        if (token == null) {

            return Map.of(
                    "success", false,
                    "message", "Access token missing"
            );
        }

        try {

            String email =
                    jwtService.extractSubject(token);

            boolean valid =
                    twoFactorService.verifyOtp(
                            email,
                            Integer.parseInt(
                                    request.getOtp()
                            )
                    );

            if (!valid) {

                return Map.of(
                        "success", false,
                        "message",
                        "Invalid or expired OTP"
                );
            }

            User user =
                    userRepository.findByEmail(email)
                            .orElseThrow();

            boolean rememberMe =
                    user.getRefreshTokenExpiry() != null
                            &&
                            user.getRefreshTokenExpiry()
                                    .isAfter(
                                            java.time.LocalDateTime
                                                    .now()
                                                    .plusDays(2)
                                    );

            LoginResponse finalResponse =
                    authService.completeTwoFactor(
                            email,
                            rememberMe
                    );

            String nextPage;

            if ("ADMIN".equalsIgnoreCase(
                    finalResponse.getRole())) {

                nextPage =
                        "/admin/dashboard";

            } else {

                nextPage =
                        "/vendor/dashboard";
            }

            return Map.of(
                    "success",
                    true,

                    "accessToken",
                    finalResponse.getAccessToken(),

                    "nextPage",
                    nextPage
            );

        } catch (Exception e) {

            e.printStackTrace();

            return Map.of(
                    "success",
                    false,

                    "message",
                    "OTP verification failed"
            );
        }
    }


    @GetMapping("/recover")
    public String recover(
            @RequestParam String email) {

        twoFactorService.regenerateSecretKey(email);

        return "redirect:/2fa/recover-qr?email="
                + email;
    }


    @GetMapping("/recover-qr")
    public String recoverQr(
            @RequestParam String email,
            Model model) {

        try {

            String qrCode =
                    twoFactorService.generateQrCode(email);

            model.addAttribute(
                    "qrCode",
                    qrCode
            );

            return "show-qr";

        } catch (Exception e) {

            e.printStackTrace();

            return "redirect:/auth/login";
        }
    }


    private String getTokenFromHeader(
            String authorizationHeader) {

        if (authorizationHeader == null ||
                !authorizationHeader.startsWith(
                        "Bearer ")) {

            return null;
        }

        return authorizationHeader.substring(7);
    }
}