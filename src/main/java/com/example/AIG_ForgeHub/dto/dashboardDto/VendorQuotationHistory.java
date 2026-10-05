package com.example.AIG_ForgeHub.dto.dashboardDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorQuotationHistory {

    private String action;

    private String version;

    private BigDecimal amount;

    private LocalDateTime date;

    private String status;
}