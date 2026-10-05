package com.example.AIG_ForgeHub.dto.dashboardDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDto {

    @NotBlank(message="Full name is required")
    private String fullName;

    @NotBlank(message="Email is required")
    @Email(message="Enter a valid email address")
    private String email;

    @NotBlank(message="Temporary password is required")
    private String password;

    private String role;
}