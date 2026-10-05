package com.example.AIG_ForgeHub.dto.authDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetRequest {

    @NotBlank
    @Size(min = 8, max = 50)
    private String newPassword;
}