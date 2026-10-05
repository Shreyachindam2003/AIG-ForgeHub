package com.example.AIG_ForgeHub.dto.dashboardDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RFQItemRequest {

    private String itemName;

    private Integer reqQty;

    private String uom;

    private LocalDate reqDeliveryDate;

    private String deliveryLocation;

    private String description;
}