package com.example.AIG_ForgeHub.service;

import com.example.AIG_ForgeHub.dto.dashboardDto.RFQCreateRequest;
import com.example.AIG_ForgeHub.dto.dashboardDto.RFQItemUpdateRequest;
import com.example.AIG_ForgeHub.dto.dashboardDto.RFQResponseDto;
import com.example.AIG_ForgeHub.dto.dashboardDto.UserResponseDto;

import java.util.List;

public interface RFQService {

    String generateRfqNo();

    String generateIndentNo();

    void saveRfq(RFQCreateRequest request, Long adminUserId, boolean draft);

    List<UserResponseDto> getAllVendors();

    List<RFQResponseDto> getAllRFQs();

    RFQResponseDto getRFQById(Long id);

    void updateRFQ(Long id,RFQCreateRequest request,boolean draft);

    void updateRFQItem(RFQItemUpdateRequest request);

    void softDeleteRFQ(Long id);

    void openToRebid(Long id);
}