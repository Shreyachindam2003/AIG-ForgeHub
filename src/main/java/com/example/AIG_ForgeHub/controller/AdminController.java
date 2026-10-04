package com.example.AIG_ForgeHub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication,Model model) {

        if(authentication==null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        boolean admin=authentication.getAuthorities()
                .stream()
                .anyMatch(authority->"ROLE_ADMIN".equals(authority.getAuthority()));

        if(!admin) {
            return "redirect:/auth/login";
        }

        model.addAttribute("email",authentication.getName());

        return "admin-dashboard";
    }
}