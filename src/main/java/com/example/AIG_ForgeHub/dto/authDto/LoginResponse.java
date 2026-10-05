package com.example.AIG_ForgeHub.dto.authDto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private Long userId;

    private String fullName;

    private String email;

    private String role;

    private String accessToken;

    private String refreshToken;

    private boolean firstTimeLogin;
}