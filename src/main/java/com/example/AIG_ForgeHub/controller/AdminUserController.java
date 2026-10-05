package com.example.AIG_ForgeHub.controller;

import com.example.AIG_ForgeHub.dto.dashboardDto.UserRequestDto;
import com.example.AIG_ForgeHub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @PostMapping("/create")
    public String createUser(@Valid @ModelAttribute("userRequest") UserRequestDto request,
                             BindingResult bindingResult,
                             Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("users", userService.getAllUsers());
            return "admin/users";
        }

        try {
            userService.createVendor(request);
            return "redirect:/admin/users?success";
        } catch (RuntimeException e) {
            log.warn("Request failed: {}", e.getMessage(), e);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("users", userService.getAllUsers());
            return "admin/users";
        }
    }
}