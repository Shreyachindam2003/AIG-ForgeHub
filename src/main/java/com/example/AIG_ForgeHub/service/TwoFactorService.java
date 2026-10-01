package com.example.AIG_ForgeHub.service;


public interface TwoFactorService {

    String generateSecretKey();

    String generateQrCode(String email);

    boolean verifyOtp(String email, int otp);

    void regenerateSecretKey(String email);
}