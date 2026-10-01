package com.example.AIG_ForgeHub.service;

public interface EmailService {

    void sendOtp(String email);

    boolean verifyOtp(String email, String otp);
}
