package com.example.AIG_ForgeHub.controller;

import com.example.AIG_ForgeHub.dto.LoginRequest;
import com.example.AIG_ForgeHub.dto.LoginResponse;
import com.example.AIG_ForgeHub.dto.VerifyEmailOtpRequest;
import com.example.AIG_ForgeHub.model.User;
import com.example.AIG_ForgeHub.repository.UserRepository;
import com.example.AIG_ForgeHub.security.JwtService;
import com.example.AIG_ForgeHub.service.AuthService;
import com.example.AIG_ForgeHub.service.EmailService;
import com.example.AIG_ForgeHub.service.TwoFactorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TwoFactorService twoFactorService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginRequest loginRequest,Model model) {

        log.info("Login attempt for email: {}",loginRequest.getEmail());

        try {

            LoginResponse loginResponse=authService.login(loginRequest);

            log.info("Login credentials verified for email: {}",loginRequest.getEmail());

            model.addAttribute("accessToken",loginResponse.getAccessToken());
            model.addAttribute("refreshToken",loginResponse.getRefreshToken());

            if (loginResponse.isFirstTimeLogin()) {

                log.info("First time login detected for email: {}",loginRequest.getEmail());

                model.addAttribute("nextPage","/2fa/qr");
            } else {

                log.info("Existing user login for email: {}",loginRequest.getEmail());

                model.addAttribute("nextPage","/2fa/proceed");
            }

            return "token-handoff";

        } catch (Exception e) {

            log.warn("Login failed for email: {}",loginRequest.getEmail());

            model.addAttribute("error","Invalid email or password");

            return "login";
        }
    }

    @PostMapping("/logout")
    @ResponseBody
    public ResponseEntity<String> logout(@RequestHeader(value="X-Refresh-Token",required=false) String refreshToken) {

        try {

            authService.logout(refreshToken);

            log.info("Logout successful");

            return ResponseEntity.ok("Logged out successfully");

        } catch (Exception e) {

            log.error("Logout failed",e);

            return ResponseEntity
                    .status(500)
                    .body("Unable to logout. Please try again.");
        }
    }

    @GetMapping("/lost-otp")
    public String lostOtpPage() {
        return "lost-otp";
    }

    @PostMapping("/lost-otp")
    public String sendEmailOtp(@RequestParam String email,Model model) {

        log.info("Email OTP requested for user: {}",email);

        try {

            emailService.sendOtp(email);

            log.info("Email OTP sent successfully for user: {}",email);

            model.addAttribute("email",email);

            model.addAttribute("success","OTP sent successfully to your email.");

            return "verify-email-otp";

        } catch (RuntimeException e) {

            log.warn("Unable to send email OTP for user: {}",email);

            model.addAttribute("error",e.getMessage());

            return "lost-otp";

        } catch (Exception e) {

            log.error("Unexpected error while sending email OTP for user: {}",email,e);

            model.addAttribute("error","Unable to send OTP. Please try again.");

            return "lost-otp";
        }
    }

    @PostMapping("/verify-email-otp")
    public String verifyEmailOtp(@Valid @ModelAttribute VerifyEmailOtpRequest request,Model model) {

        log.info("Email OTP verification attempt for user: {}",request.getEmail());

        boolean verified=emailService.verifyOtp(
                request.getEmail(),
                request.getOtp()
        );

        if (!verified) {

            log.warn("Invalid or expired email OTP for user: {}",request.getEmail());

            model.addAttribute("email",request.getEmail());
            model.addAttribute("error","Invalid or expired OTP");

            return "verify-email-otp";
        }

        log.info("Email OTP verified successfully for user: {}",request.getEmail());

        User user=userRepository.findByEmail(request.getEmail())
                .orElseThrow(()->new RuntimeException("User not found"));

        twoFactorService.regenerateSecretKey(request.getEmail());

        log.info("2FA secret regenerated for user: {}",request.getEmail());

        String recoveryToken=jwtService.generateRecoveryToken(
                user.getEmail(),
                user.getRole()
        );

        String qrCode=twoFactorService.generateQrCode(
                user.getEmail()
        );

        model.addAttribute("qrCode",qrCode);
        model.addAttribute("accessToken",recoveryToken);
        model.addAttribute("refreshToken","");

        log.info("Recovery QR generated successfully for user: {}",request.getEmail());

        return "show-qr";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "lost-otp";
    }

    @PostMapping("/refresh")
    @ResponseBody
    public ResponseEntity<?> refresh(@RequestHeader(value="X-Refresh-Token",required=false) String refreshToken) {

        try {

            LoginResponse response=
                    authService.refreshAccessToken(refreshToken);

            log.info("Access token refreshed successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            log.warn("Access token refresh failed");

            return ResponseEntity
                    .status(401)
                    .body("Invalid or expired refresh token");
        }
    }
}