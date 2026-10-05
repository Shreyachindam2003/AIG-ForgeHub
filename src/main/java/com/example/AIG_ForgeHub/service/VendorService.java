package com.example.AIG_ForgeHub.service;

import com.example.AIG_ForgeHub.dto.dashboardDto.*;

import java.util.List;

public interface VendorService {

    UserResponseDto getVendor(Long vendorId);

    List<RFQResponseDto> getOpenRfqs(Long vendorId);

    RFQResponseDto getAssignedRfq(Long rfqId,Long vendorId);

    void submitQuotation(Long rfqId, Long vendorId, VendorQuotationRequest request);

    List<VendorQuotationResponseDto> getMySubmissions(Long vendorId);

    VendorQuotationResponseDto getMySubmission(Long quotationId,Long vendorId);

    List<FinalizedQuotationResponseDto> getFinalizedQuotations(Long vendorId);

    List<VendorQuotationResponseDto> getAllVendorQuotations();

    VendorQuotationResponseDto getQuotationForAdmin(Long quotationId);

    void finalizeQuotation(Long quotationId);

    List<FinalizedQuotationResponseDto> getAllFinalizedQuotations();
}