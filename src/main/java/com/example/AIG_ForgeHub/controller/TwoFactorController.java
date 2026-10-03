package com.example.AIG_ForgeHub.controller;

import com.example.AIG_ForgeHub.dto.LoginResponse;
import com.example.AIG_ForgeHub.dto.VerifyOtpRequest;
import com.example.AIG_ForgeHub.security.JwtService;
import com.example.AIG_ForgeHub.service.AuthService;
import com.example.AIG_ForgeHub.service.TwoFactorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/2fa")
@RequiredArgsConstructor
@Slf4j
public class TwoFactorController {

    private final JwtService jwtService;
    private final TwoFactorService twoFactorService;
    private final AuthService authService;

    @GetMapping("/qr")
    public String qrPage(@RequestHeader(value="Authorization",required=false) String authorizationHeader,@RequestHeader(value="X-Refresh-Token",required=false) String refreshToken,Model model) {
        log.info("2FA QR page requested");

        String token=getTokenFromHeader(authorizationHeader);

        if(token==null) {
            log.warn("2FA QR page rejected because access token is missing");
            return "redirect:/auth/login";
        }

        try {
            if(!jwtService.isTokenValid(token)) {
                log.warn("2FA QR page rejected because token is invalid");
                return "redirect:/auth/login";
            }

            if(!jwtService.isAccessToken(token) && !jwtService.isRecoveryToken(token)) {
                log.warn("2FA QR page rejected because token type is invalid");
                return "redirect:/auth/login";
            }

            String email=jwtService.extractSubject(token);

            log.info("Generating 2FA QR code for user: {}",email);

            String qrCode=twoFactorService.generateQrCode(email);

            model.addAttribute("qrCode",qrCode);
            model.addAttribute("accessToken",token);
            model.addAttribute("refreshToken",refreshToken==null ? "" : refreshToken);

            log.info("2FA QR page loaded successfully for user: {}",email);

            return "show-qr";

        } catch(Exception e) {
            log.error("Failed to load 2FA QR page",e);
            return "redirect:/auth/login";
        }
    }

    @PostMapping("/verify")
    @ResponseBody
    public Map<String,Object> verifyOtp(@Valid @ModelAttribute VerifyOtpRequest request,@RequestHeader(value="Authorization",required=false) String authorizationHeader,@RequestHeader(value="X-Refresh-Token",required=false) String refreshToken) {
        log.info("2FA OTP verification request received");

        String token=getTokenFromHeader(authorizationHeader);

        if(token==null) {
            log.warn("2FA OTP verification rejected because access token is missing");

            return Map.of(
                    "success",false,
                    "message","Access token missing"
            );
        }

        try {
            if(!jwtService.isTokenValid(token)) {
                log.warn("2FA OTP verification rejected because token is invalid");

                return Map.of(
                        "success",false,
                        "message","Invalid or expired token"
                );
            }

            boolean normalLoginToken=jwtService.isAccessToken(token);
            boolean recoveryToken=jwtService.isRecoveryToken(token);

            if(!normalLoginToken && !recoveryToken) {
                log.warn("2FA OTP verification rejected because token type is invalid");

                return Map.of(
                        "success",false,
                        "message","Invalid token type"
                );
            }

            String email=jwtService.extractSubject(token);

            log.info("Verifying 2FA OTP for user: {}",email);

            boolean valid=twoFactorService.verifyOtp(
                    email,
                    Integer.parseInt(request.getOtp())
            );

            if(!valid) {
                log.warn("Invalid 2FA OTP for user: {}",email);

                return Map.of(
                        "success",false,
                        "message","Invalid or expired OTP"
                );
            }

            LoginResponse finalResponse;

            if(recoveryToken) {
                log.info("Completing 2FA recovery login for user: {}",email);
                finalResponse=authService.completeTwoFactor(email,null);
            } else {
                log.info("Completing normal 2FA login for user: {}",email);
                finalResponse=authService.completeTwoFactor(email,refreshToken);
            }

            String nextPage;

            if("ADMIN".equalsIgnoreCase(finalResponse.getRole())) {
                nextPage="/admin/dashboard";
            } else {
                nextPage="/vendor/dashboard";
            }

            log.info("2FA verification successful for user: {}",email);

            return Map.of(
                    "success",true,
                    "accessToken",finalResponse.getAccessToken(),
                    "refreshToken",finalResponse.getRefreshToken(),
                    "nextPage",nextPage
            );

        } catch(Exception e) {
            log.error("2FA OTP verification failed",e);

            return Map.of(
                    "success",false,
                    "message","OTP verification failed"
            );
        }
    }

    @PostMapping("/proceed")
    public String proceedToVerify(@RequestParam String accessToken,@RequestParam(required=false,defaultValue="") String refreshToken,Model model) {
        log.info("Proceed to 2FA verification requested");

        try {
            if(!jwtService.isTokenValid(accessToken)) {
                log.warn("Proceed to 2FA verification rejected because token is invalid");
                return "redirect:/auth/login";
            }

            if(!jwtService.isAccessToken(accessToken) && !jwtService.isRecoveryToken(accessToken)) {
                log.warn("Proceed to 2FA verification rejected because token type is invalid");
                return "redirect:/auth/login";
            }

            model.addAttribute("accessToken",accessToken);
            model.addAttribute("refreshToken",refreshToken);

            log.info("Proceed to 2FA verification successful");

            return "verify-otp";

        } catch(Exception e) {
            log.error("Failed to open 2FA verification page",e);
            return "redirect:/auth/login";
        }
    }

    private String getTokenFromHeader(String authorizationHeader) {
        if(authorizationHeader==null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        return authorizationHeader.substring(7);
    }
}