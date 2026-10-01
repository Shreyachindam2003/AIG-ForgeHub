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

        String token = getTokenFromHeader(authorizationHeader);

        if (token == null) {

            return "redirect:/auth/login";
        }

        try {

            String email =
                    jwtService.extractSubject(token);

            if (jwtService.isMfaVerified(token)) {

                return redirectBasedOnRole(email);
            }

            String qrCode =
                    twoFactorService.generateQrCode(email);

            model.addAttribute(
                    "qrCode",
                    qrCode
            );

            return "show-qr";

        } catch (Exception e) {

            return "redirect:/auth/login";
        }
    }


    @GetMapping("/verify")
    public String verifyPage(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader) {

        String token =
                getTokenFromHeader(authorizationHeader);

        if (token == null) {

            return "redirect:/auth/login";
        }

        return "verify-otp";
    }


    @PostMapping("/verify")
    public String verifyOtp(
            @Valid @ModelAttribute VerifyOtpRequest request,
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

            boolean valid =
                    twoFactorService.verifyOtp(
                            email,
                            Integer.parseInt(
                                    request.getOtp()
                            )
                    );

            if (!valid) {

                model.addAttribute(
                        "error",
                        "Invalid or expired OTP"
                );

                return "verify-otp";
            }

            User user =
                    userRepository.findByEmail(email)
                            .orElseThrow();

            /*
             * Remember me information
             * refresh token expiry se derive kar sakte hain.
             */
            boolean rememberMe =
                    user.getRefreshTokenExpiry() != null &&
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

            return redirectBasedOnRole(
                    finalResponse.getRole()
            );

        } catch (Exception e) {

            model.addAttribute(
                    "error",
                    "OTP verification failed"
            );

            return "verify-otp";
        }
    }


    @GetMapping("/proceed")
    public String proceed() {

        return "redirect:/2fa/verify";
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

        String qrCode =
                twoFactorService.generateQrCode(email);

        model.addAttribute(
                "qrCode",
                qrCode
        );

        return "show-qr";
    }


    private String redirectBasedOnRole(
            String role) {

        if ("ADMIN".equalsIgnoreCase(role)) {

            return "redirect:/admin/dashboard";
        }

        return "redirect:/vendor/dashboard";
    }


    private String getTokenFromHeader(
            String authorizationHeader) {

        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            return null;
        }

        return authorizationHeader.substring(7);
    }
}