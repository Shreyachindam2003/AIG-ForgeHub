package com.example.AIG_ForgeHub.controller;

import com.example.AIG_ForgeHub.dto.dashboardDto.RFQResponseDto;
import com.example.AIG_ForgeHub.dto.dashboardDto.UserRequestDto;
import com.example.AIG_ForgeHub.dto.dashboardDto.VendorQuotationResponseDto;
import com.example.AIG_ForgeHub.enums.RFQStatus;
import com.example.AIG_ForgeHub.service.RFQService;
import com.example.AIG_ForgeHub.service.UserService;
import com.example.AIG_ForgeHub.service.VendorService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin")
@AllArgsConstructor
public class AdminSectionController {

    private final UserService userService;

    private final VendorService vendorService;

    private final RFQService rfqService;

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

        List<RFQResponseDto> rfqs=rfqService.getAllRFQs();

        long totalRFQs=rfqs.size();

        long draftRFQs=rfqs.stream()
                .filter(rfq->rfq.getStatus()==RFQStatus.DRAFT)
                .count();

        long openRFQs=rfqs.stream()
                .filter(rfq->rfq.getStatus()==RFQStatus.OPEN || rfq.getStatus()==RFQStatus.REOPENED)
                .count();

        long closedRFQs=rfqs.stream()
                .filter(rfq->rfq.getStatus()==RFQStatus.CLOSED)
                .count();

        long finalizedRFQs=rfqs.stream()
                .filter(rfq->rfq.getStatus()==RFQStatus.FINALIZED)
                .count();

        model.addAttribute("email",authentication.getName());
        model.addAttribute("totalRFQs",totalRFQs);
        model.addAttribute("draftRFQs",draftRFQs);
        model.addAttribute("openRFQs",openRFQs);
        model.addAttribute("closedRFQs",closedRFQs);
        model.addAttribute("finalizedRFQs",finalizedRFQs);
        model.addAttribute("recentRFQs",rfqs.stream().limit(5).toList());

        return "admin/dashboard";
    }

    @GetMapping("/vendor-quotation")
    public String vendorQuotation(Model model) {

        model.addAttribute("quotations", vendorService.getAllVendorQuotations());

        return "admin/vendor-quotation";
    }

    @GetMapping("/vendor-quotation/details/{quotationId}")
    @ResponseBody
    public ResponseEntity<VendorQuotationResponseDto> vendorQuotationDetails(@PathVariable Long quotationId) {
        return ResponseEntity.ok(vendorService.getQuotationForAdmin(quotationId));
    }

    @PostMapping("/vendor-quotation/finalize/{quotationId}")
    public String finalizeVendorQuotation(@PathVariable Long quotationId, RedirectAttributes redirectAttributes) {

        try {
            vendorService.finalizeQuotation(quotationId);

            redirectAttributes.addFlashAttribute("success", "Vendor quotation finalized successfully.");

        } catch(RuntimeException e) {
            log.warn("Request failed: {}",e.getMessage(),e);

            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/vendor-quotation";
    }

    @GetMapping("/finance-quotation")
    public String financeQuotation(Model model) {

        model.addAttribute("quotations", vendorService.getAllFinalizedQuotations());

        return "admin/finance-quotation";
    }

    @GetMapping("/users")
    public String users(Model model) {

        model.addAttribute("userRequest", new UserRequestDto());

        model.addAttribute("users", userService.getAllUsers());

        return "admin/users";
    }
}