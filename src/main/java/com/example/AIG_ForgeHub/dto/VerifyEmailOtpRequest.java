package com.example.AIG_ForgeHub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyEmailOtpRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String otp;
}