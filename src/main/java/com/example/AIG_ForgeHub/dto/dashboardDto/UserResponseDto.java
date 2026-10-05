package com.example.AIG_ForgeHub.dto.dashboardDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private Long userId;

    private String fullName;

    private String email;

    private String role;

    private boolean isFirstTimeLogin;

    private boolean totpConfigured;

    private String token;

    private String refreshToken;
}