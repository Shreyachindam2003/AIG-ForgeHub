package com.example.AIG_ForgeHub.controller;

import com.example.AIG_ForgeHub.dto.LoginRequest;
import com.example.AIG_ForgeHub.dto.LoginResponse;
import com.example.AIG_ForgeHub.dto.VerifyEmailOtpRequest;
import com.example.AIG_ForgeHub.repository.UserRepository;
import com.example.AIG_ForgeHub.security.JwtService;
import com.example.AIG_ForgeHub.service.AuthService;
import com.example.AIG_ForgeHub.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final EmailService emailService;

    private final UserRepository userRepository;

    private final JwtService jwtService;


    @GetMapping("/login")
    public String loginPage() {

        return "login";
    }


    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute LoginRequest loginRequest,
            Model model) {

        try {

            LoginResponse loginResponse =
                    authService.login(loginRequest);

            if (loginResponse.isFirstTimeLogin()) {

                return "redirect:/2fa/qr";
            }

            return "redirect:/2fa/verify";

        } catch (Exception e) {

            model.addAttribute(
                    "error",
                    "Invalid email or password"
            );

            return "login";
        }
    }


    @PostMapping("/logout")
    public String logout(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorizationHeader) {

        try {

            if (authorizationHeader != null &&
                    authorizationHeader.startsWith("Bearer ")) {

                String accessToken =
                        authorizationHeader.substring(7);

                String email =
                        jwtService.extractSubject(accessToken);

                authService.logout(email);
            }

        } catch (Exception ignored) {
        }

        return "redirect:/auth/login";
    }


    @GetMapping("/lost-otp")
    public String lostOtpPage() {

        return "lost-otp";
    }


    @PostMapping("/lost-otp")
    public String sendEmailOtp(
            @RequestParam String email,
            Model model) {

        try {

            emailService.sendOtp(email);

            model.addAttribute(
                    "email",
                    email
            );

            return "verify-email-otp";

        } catch (Exception e) {

            model.addAttribute(
                    "error",
                    "Email not found"
            );

            return "lost-otp";
        }
    }


    @PostMapping("/verify-email-otp")
    public String verifyEmailOtp(
            @Valid @ModelAttribute VerifyEmailOtpRequest request,
            Model model) {

        boolean verified =
                emailService.verifyOtp(
                        request.getEmail(),
                        request.getOtp()
                );

        if (!verified) {

            model.addAttribute(
                    "email",
                    request.getEmail()
            );

            model.addAttribute(
                    "error",
                    "Invalid or expired OTP"
            );

            return "verify-email-otp";
        }

        return "redirect:/2fa/recover?email="
                + request.getEmail();
    }


    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {

        return "lost-otp";
    }
}