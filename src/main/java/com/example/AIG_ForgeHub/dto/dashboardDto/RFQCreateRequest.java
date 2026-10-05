package com.example.AIG_ForgeHub.dto.dashboardDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RFQCreateRequest {

    private String contactPerson;

    private String mobile;

    private LocalDate bidDate;

    private LocalDate expiryDateOfBid;

    private List<RFQItemRequest> items = new ArrayList<>();

    private List<Long> vendorIds = new ArrayList<>();
}