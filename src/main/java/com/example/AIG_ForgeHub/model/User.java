package com.example.AIG_ForgeHub.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserId")
    private Long userId;

    @Column(name = "FullName")
    private String fullName;

    @Column(name = "Email")
    private String email;

    @Column(name = "PasswordHash")
    private String passwordHash;

    @Column(name = "Role")
    private String role;

    @Column(name = "IsFirstTimeLogin")
    private Boolean isFirstTimeLogin;

    @Column(name = "SecretKey")
    private String secretKey;

    @Column(name = "RefreshTokenHash")
    private String refreshTokenHash;

    @Column(name = "RefreshTokenExpiry")
    private LocalDateTime refreshTokenExpiry;

    @Column(name = "EmailOtpHash")
    private String emailOtpHash;

    @Column(name = "EmailOtpExpiry")
    private LocalDateTime emailOtpExpiry;
}