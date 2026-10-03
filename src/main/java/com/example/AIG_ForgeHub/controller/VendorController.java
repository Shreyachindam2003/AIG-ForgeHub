package com.example.AIG_ForgeHub.controller;

import com.example.AIG_ForgeHub.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/vendor")
@RequiredArgsConstructor
public class VendorController {

    private final JwtService jwtService;

    @PostMapping("/dashboard")
    public String dashboard(@RequestParam String accessToken,@RequestParam(required=false,defaultValue="") String refreshToken,Model model) {

        try {
            if(!jwtService.isTokenValid(accessToken)) {
                return "redirect:/auth/login";
            }

            if(!jwtService.isAccessToken(accessToken)) {
                return "redirect:/auth/login";
            }

            if(!jwtService.isMfaVerified(accessToken)) {
                return "redirect:/auth/login";
            }

            String email=jwtService.extractSubject(accessToken);
            String role=jwtService.extractRole(accessToken);

            if(!"VENDOR".equalsIgnoreCase(role)) {
                return "redirect:/auth/login";
            }

            model.addAttribute("email",email);
            model.addAttribute("accessToken",accessToken);
            model.addAttribute("refreshToken",refreshToken);

            return "vendor-dashboard";

        } catch(Exception e) {
            return "redirect:/auth/login";
        }
    }
}