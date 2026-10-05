package com.example.AIG_ForgeHub.controller;

import com.example.AIG_ForgeHub.dto.authDto.LoginResponse;
import com.example.AIG_ForgeHub.dto.authDto.VerifyOtpRequest;
import com.example.AIG_ForgeHub.security.CookieUtil;
import com.example.AIG_ForgeHub.service.AuthService;
import com.example.AIG_ForgeHub.service.TwoFactorService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/2fa")
@RequiredArgsConstructor
@Slf4j
public class TwoFactorController {

    private final TwoFactorService twoFactorService;
    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${jwt.remember-me-expiration-ms}")
    private long rememberMeExpirationMs;

    @PostMapping("/proceed")
    public String proceedToVerify(@RequestParam String email, @RequestParam(defaultValue="false") boolean rememberMe,
            Model model) {

        log.info("Proceed to 2FA verification requested for user: {}",email);

        try {

            model.addAttribute("email",email);
            model.addAttribute("rememberMe",rememberMe);

            return "verify-otp";

        } catch(Exception e) {

            log.error("Failed to open 2FA verification page",e);

            return "redirect:/auth/login";
        }
    }

    @PostMapping("/verify")
    public String verifyOtp(@Valid @ModelAttribute VerifyOtpRequest request, @RequestParam String email,
            @RequestParam(defaultValue="false") boolean rememberMe, HttpServletResponse response,
            Model model) {

        log.info("2FA OTP verification request received for user: {}",email);

        try {
            boolean valid=twoFactorService.verifyOtp(email, Integer.parseInt(request.getOtp()));
            if(!valid) {

                log.warn("Invalid 2FA OTP for user: {}",email);

                model.addAttribute("email",email);
                model.addAttribute("rememberMe",rememberMe);
                model.addAttribute("error","Invalid or expired OTP");

                return "verify-otp";
            }

            LoginResponse finalResponse= authService.completeTwoFactor(email, rememberMe);

            long refreshExpiration=rememberMe ? rememberMeExpirationMs : refreshTokenExpirationMs;

            cookieUtil.addAccessTokenCookie(response, finalResponse.getAccessToken(), accessTokenExpirationMs);

            cookieUtil.addRefreshTokenCookie(response, finalResponse.getRefreshToken(), refreshExpiration);

            log.info("2FA verification successful for user: {}",email);

            if("ADMIN".equalsIgnoreCase(finalResponse.getRole())) {
                return "redirect:/admin/dashboard";
            }

            return "redirect:/vendor/dashboard";

        } catch(Exception e) {

            log.error("2FA OTP verification failed for user: {}",email,e);

            model.addAttribute("email",email);
            model.addAttribute("rememberMe",rememberMe);
            model.addAttribute("error","OTP verification failed");

            return "verify-otp";
        }
    }
}