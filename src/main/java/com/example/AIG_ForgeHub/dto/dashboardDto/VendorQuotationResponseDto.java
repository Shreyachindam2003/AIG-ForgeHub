package com.example.AIG_ForgeHub.dto.dashboardDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorQuotationResponseDto {

    private Long quotationId;

    private String bidNo;

    private Long rfqId;

    private String rfqNo;

    private String indentNo;

    private Long vendorId;

    private String vendorName;

    private String vendorEmail;

    private BigDecimal quotedAmount;

    private BigDecimal subtotal;

    private BigDecimal gstRate;

    private BigDecimal gstAmount;

    private BigDecimal grandTotal;

    private LocalDate deliveryDate;

    private String paymentTerms;

    private String status;

    private LocalDateTime submittedDate;

    private List<VendorQuotationItemResponseDto> items;

    private List<VendorQuotationHistory> history;
}