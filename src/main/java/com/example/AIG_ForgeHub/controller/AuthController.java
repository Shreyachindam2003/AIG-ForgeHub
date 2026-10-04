package com.example.AIG_ForgeHub.controller;

import com.example.AIG_ForgeHub.dto.LoginRequest;
import com.example.AIG_ForgeHub.dto.LoginResponse;
import com.example.AIG_ForgeHub.dto.VerifyEmailOtpRequest;
import com.example.AIG_ForgeHub.model.User;
import com.example.AIG_ForgeHub.repository.UserRepository;
import com.example.AIG_ForgeHub.security.CookieUtil;
import com.example.AIG_ForgeHub.service.AuthService;
import com.example.AIG_ForgeHub.service.EmailService;
import com.example.AIG_ForgeHub.service.TwoFactorService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final TwoFactorService twoFactorService;
    private final CookieUtil cookieUtil;

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${jwt.remember-me-expiration-ms}")
    private long rememberMeExpirationMs;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute LoginRequest loginRequest,
            Model model) {

        log.info("Login attempt for email: {}",loginRequest.getEmail());

        try {

            LoginResponse loginResponse=authService.login(loginRequest);

            log.info("Password authentication verified for email: {}",loginRequest.getEmail());

            model.addAttribute("email",loginResponse.getEmail());
            model.addAttribute("rememberMe",loginRequest.isRememberMe());

            if(loginResponse.isFirstTimeLogin()) {

                log.info("First time login detected for email: {}",loginRequest.getEmail());

                String qrCode=twoFactorService.generateQrCode(
                        loginResponse.getEmail()
                );

                model.addAttribute("qrCode",qrCode);

                return "show-qr";

            } else {

                log.info("Existing user requires 2FA verification for email: {}",loginRequest.getEmail());

                return "verify-otp";
            }

        } catch(Exception e) {

            log.warn("Login failed for email: {}",loginRequest.getEmail());

            model.addAttribute("error","Invalid email or password");

            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(
            @CookieValue(value="refreshToken",required=false) String refreshToken,
            HttpServletResponse response) {

        try {

            authService.logout(refreshToken);

            cookieUtil.clearAccessTokenCookie(response);
            cookieUtil.clearRefreshTokenCookie(response);

            log.info("Logout successful");

            return "redirect:/auth/login";

        } catch(Exception e) {

            log.error("Logout failed",e);

            cookieUtil.clearAccessTokenCookie(response);
            cookieUtil.clearRefreshTokenCookie(response);

            return "redirect:/auth/login";
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

        } catch(RuntimeException e) {

            log.warn("Unable to send email OTP for user: {}",email);

            model.addAttribute("error",e.getMessage());

            return "lost-otp";

        } catch(Exception e) {

            log.error("Unexpected error while sending email OTP to user: {}",email,e);

            model.addAttribute("error","Unable to send OTP. Please try again.");

            return "lost-otp";
        }
    }

    @PostMapping("/verify-email-otp")
    public String verifyEmailOtp(
            @Valid @ModelAttribute VerifyEmailOtpRequest request,
            Model model) {

        log.info("Email OTP verification attempt for user: {}",request.getEmail());

        boolean verified=emailService.verifyOtp(
                request.getEmail(),
                request.getOtp()
        );

        if(!verified) {

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

        String qrCode=twoFactorService.generateQrCode(
                user.getEmail()
        );

        model.addAttribute("qrCode",qrCode);
        model.addAttribute("email",user.getEmail());
        model.addAttribute("rememberMe",false);

        log.info("Recovery QR generated successfully for user: {}",request.getEmail());

        return "show-qr";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "lost-otp";
    }

    @PostMapping("/refresh")
    @ResponseBody
    public String refresh(
            @CookieValue(value="refreshToken",required=false) String refreshToken,
            HttpServletResponse response) {

        try {

            LoginResponse loginResponse=
                    authService.refreshAccessToken(refreshToken);

            long refreshExpiration=loginResponse.getRefreshToken()!=null
                    ? refreshTokenExpirationMs
                    : refreshTokenExpirationMs;

            cookieUtil.addAccessTokenCookie(
                    response,
                    loginResponse.getAccessToken(),
                    accessTokenExpirationMs
            );

            log.info("Access token refreshed successfully");

            return "Access token refreshed successfully";

        } catch(Exception e) {

            log.warn("Access token refresh failed");

            cookieUtil.clearAccessTokenCookie(response);

            return "Invalid or expired refresh token";
        }
    }
}