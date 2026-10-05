package com.example.AIG_ForgeHub.dto.dashboardDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RFQItemResponseDto {

    private Long itemId;

    private Integer rfqLineNo;

    private String itemNo;

    private String itemName;

    private Integer reqQty;

    private String uom;

    private LocalDate reqDeliveryDate;

    private String deliveryLocation;

    private String factoryCode;

    private String description;
}