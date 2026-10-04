package com.example.AIG_ForgeHub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/vendor")
@RequiredArgsConstructor
public class VendorController {

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication,Model model) {

        if(authentication==null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        boolean vendor=authentication.getAuthorities()
                .stream()
                .anyMatch(authority->"ROLE_VENDOR".equals(authority.getAuthority()));

        if(!vendor) {
            return "redirect:/auth/login";
        }

        model.addAttribute("email",authentication.getName());

        return "vendor-dashboard";
    }
}