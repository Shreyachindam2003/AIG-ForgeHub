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
import org.springframework.http.ResponseEntity;
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
    public String login(@Valid @ModelAttribute LoginRequest loginRequest, Model model) {
        System.out.println("========== LOGIN CONTROLLER START ==========");
        System.out.println("EMAIL = " + loginRequest.getEmail());

        try {
            LoginResponse loginResponse = authService.login(loginRequest);

            System.out.println("========== LOGIN SUCCESS ==========");
            System.out.println("EMAIL = " + loginResponse.getEmail());
            System.out.println("ROLE = " + loginResponse.getRole());
            System.out.println("FIRST LOGIN = " + loginResponse.isFirstTimeLogin());
            System.out.println("TOKEN GENERATED = " + (loginResponse.getAccessToken() != null));

            model.addAttribute("accessToken", loginResponse.getAccessToken());
            model.addAttribute("refreshToken", loginResponse.getRefreshToken());

            if ("ADMIN".equalsIgnoreCase(loginResponse.getRole())) {
                System.out.println("ADMIN LOGIN");
                System.out.println("NEXT PAGE = /admin/dashboard");
                model.addAttribute("nextPage", "/admin/dashboard");
            } else if ("VENDOR".equalsIgnoreCase(loginResponse.getRole())) {
                if (loginResponse.isFirstTimeLogin()) {
                    System.out.println("VENDOR FIRST LOGIN");
                    System.out.println("NEXT PAGE = /2fa/qr");
                    model.addAttribute("nextPage", "/2fa/qr");
                } else {
                    System.out.println("VENDOR NORMAL LOGIN");
                    System.out.println("NEXT PAGE = /2fa/verify");
                    model.addAttribute("nextPage", "/2fa/verify");
                }
            } else {
                System.out.println("UNKNOWN ROLE = " + loginResponse.getRole());
                model.addAttribute("error", "Invalid user role");
                return "login";
            }

            return "token-handoff";

        } catch (Exception e) {
            System.out.println("========== LOGIN FAILED ==========");
            e.printStackTrace();
            model.addAttribute("error", "Invalid email or password");
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String accessToken = authorizationHeader.substring(7);
                String email = jwtService.extractSubject(accessToken);
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
    public String sendEmailOtp(@RequestParam String email, Model model) {
        try {
            emailService.sendOtp(email);
            model.addAttribute("email", email);
            return "verify-email-otp";
        } catch (RuntimeException e) {
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            return "lost-otp";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Unable to send OTP. Please try again.");
            return "lost-otp";
        }
    }

    @PostMapping("/verify-email-otp")
    public String verifyEmailOtp(@Valid @ModelAttribute VerifyEmailOtpRequest request, Model model) {
        boolean verified = emailService.verifyOtp(request.getEmail(), request.getOtp());

        if (!verified) {
            model.addAttribute("email", request.getEmail());
            model.addAttribute("error", "Invalid or expired OTP");
            return "verify-email-otp";
        }

        return "redirect:/2fa/recover?email=" + request.getEmail();
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "lost-otp";
    }

    @PostMapping("/refresh")
    @ResponseBody
    public ResponseEntity<?> refresh(@RequestParam String refreshToken) {
        try {
            LoginResponse response = authService.refreshAccessToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(
                    java.util.Map.of(
                            "success", false,
                            "message", "Invalid or expired refresh token"
                    )
            );
        }
    }
}