package com.example.AIG_ForgeHub.dto.dashboardDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalizedQuotationResponseDto {
    private Long finalId;
    private String rfqNo;
    private String indentNo;
    private List<RFQItemResponseDto> items;
    private String vendorName;
    private Long quotationId;
    private BigDecimal quotedAmount;
    private LocalDateTime finalizedDate;
    private String bidNo;
}