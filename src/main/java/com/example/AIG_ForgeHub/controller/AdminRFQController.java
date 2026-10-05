package com.example.AIG_ForgeHub.controller;

import com.example.AIG_ForgeHub.dto.dashboardDto.RFQCreateRequest;
import com.example.AIG_ForgeHub.dto.dashboardDto.RFQItemUpdateRequest;
import com.example.AIG_ForgeHub.dto.dashboardDto.RFQResponseDto;
import com.example.AIG_ForgeHub.service.RFQService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/admin/rfq")
@RequiredArgsConstructor
public class AdminRFQController {

    private final RFQService rfqService;

    private static final Long TEST_ADMIN_ID=1L;

    @GetMapping("/add")
    public String showAddRFQ(Model model) {

        model.addAttribute("rfqRequest",new RFQCreateRequest());
        model.addAttribute("vendors",rfqService.getAllVendors());

        return "admin/add-rfq";
    }

    @PostMapping("/save")
    public String saveRFQ(
            @ModelAttribute("rfqRequest") RFQCreateRequest request,
            @RequestParam(name="action",defaultValue="submit") String action,
            Model model
    ) {

        boolean draft=action.equalsIgnoreCase("draft");

        try {
            rfqService.saveRfq(request,TEST_ADMIN_ID,draft);
            return "redirect:/admin/rfq/list";

        } catch(Exception e) {
            log.error("Request failed unexpectedly",e);

            model.addAttribute("error",e.getMessage());
            model.addAttribute("vendors",rfqService.getAllVendors());

            return "admin/add-rfq";
        }
    }

    @GetMapping("/list")
    public String showRFQList(Model model) {

        model.addAttribute("rfqs",rfqService.getAllRFQs());

        return "admin/rfq-list";
    }

    @GetMapping("/details/{id}")
    @ResponseBody
    public RFQResponseDto getRFQDetails(@PathVariable Long id) {
        return rfqService.getRFQById(id);
    }

    @PostMapping("/update/{id}")
    public String updateRFQ(
            @PathVariable Long id,
            @ModelAttribute RFQCreateRequest request,
            @RequestParam(name="action",defaultValue="submit") String action
    ) {

        boolean draft=action.equalsIgnoreCase("draft");

        rfqService.updateRFQ(id,request,draft);

        return "redirect:/admin/rfq/list";
    }

    @PostMapping("/item/update")
    @ResponseBody
    public ResponseEntity<String> updateRFQItem(@ModelAttribute RFQItemUpdateRequest request) {

        rfqService.updateRFQItem(request);
        return ResponseEntity.ok("success");
    }

    @PostMapping("/delete/{id}")
    public String deleteRFQ(@PathVariable Long id) {

        rfqService.softDeleteRFQ(id);

        return "redirect:/admin/rfq/list";
    }

    @PostMapping("/rebid/{id}")
    public String openToRebid(@PathVariable Long id) {

        rfqService.openToRebid(id);

        return "redirect:/admin/rfq/list";
    }
}