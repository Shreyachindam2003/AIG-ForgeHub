package com.example.AIG_ForgeHub.dto.dashboardDto;

import com.example.AIG_ForgeHub.enums.RFQStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RFQResponseDto {

    private Long rfqId;

    private String rfqNo;

    private String indentNo;

    private String contactPerson;

    private String mobile;

    private LocalDateTime bidDate;

    private LocalDateTime expiryDateOfBid;

    private RFQStatus status;

    private boolean deleted;

    private List<RFQItemResponseDto> items;
}