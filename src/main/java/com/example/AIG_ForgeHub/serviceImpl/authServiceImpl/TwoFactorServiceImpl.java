package com.example.AIG_ForgeHub.serviceImpl.authServiceImpl;

import com.example.AIG_ForgeHub.entity.User;
import com.example.AIG_ForgeHub.repository.UserRepository;
import com.example.AIG_ForgeHub.service.TwoFactorService;
import com.example.AIG_ForgeHub.util.QrCodeUtil;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorServiceImpl implements TwoFactorService {

    private final UserRepository userRepository;
    private final QrCodeUtil qrCodeUtil;

    @Override
    public String generateSecretKey() {

        GoogleAuthenticator googleAuthenticator=new GoogleAuthenticator();
        GoogleAuthenticatorKey key=googleAuthenticator.createCredentials();

        log.debug("2FA secret key generated successfully");

        return key.getKey();
    }

    @Override
    public String generateQrCode(String email) {

        log.info("QR code generation requested for user: {}",email);

        User user=userRepository.findByEmail(email)
                .orElseThrow(()->{
                    log.warn("User not found while generating QR code: {}",email);
                    return new RuntimeException("User not found");
                });

        String secretKey=user.getSecretKey();

        if (secretKey==null || secretKey.isBlank()) {

            log.warn("2FA secret key not found for user: {}",email);

            throw new RuntimeException("Secret key not found");
        }

        String issuer="AIG ForgeHub";
        String encodedIssuer=URLEncoder.encode(issuer,StandardCharsets.UTF_8);
        String encodedEmail=URLEncoder.encode(email,StandardCharsets.UTF_8);

        String qrText="otpauth://totp/"
                +encodedIssuer
                +":"
                +encodedEmail
                +"?secret="
                +secretKey
                +"&issuer="
                +encodedIssuer;

        try {

            String qrCode=qrCodeUtil.generateQrCode(qrText);

            log.info("2FA QR code generated successfully for user: {}",email);

            return qrCode;

        } catch (Exception e) {

            log.error("Failed to generate 2FA QR code for user: {}",email,e);

            throw new RuntimeException("Unable to generate QR code",e);
        }
    }

    @Override
    public boolean verifyOtp(String email,int otp) {

        log.info("2FA OTP verification requested for user: {}",email);

        User user=userRepository.findByEmail(email)
                .orElseThrow(()->{
                    log.warn("User not found during 2FA verification: {}",email);
                    return new RuntimeException("User not found");
                });

        String secretKey=user.getSecretKey();

        if (secretKey==null || secretKey.isBlank()) {

            log.warn("2FA secret key not found for user: {}",email);

            return false;
        }

        GoogleAuthenticator googleAuthenticator=new GoogleAuthenticator();

        boolean valid=googleAuthenticator.authorize(secretKey,otp);

        if (valid) {
            log.info("2FA OTP verified successfully for user: {}",email);
        } else {
            log.warn("Invalid 2FA OTP for user: {}",email);
        }

        return valid;
    }

    @Override
    public void regenerateSecretKey(String email) {

        log.info("2FA secret regeneration requested for user: {}",email);

        User user=userRepository.findByEmail(email)
                .orElseThrow(()->{
                    log.warn("User not found while regenerating 2FA secret: {}",email);
                    return new RuntimeException("User not found");
                });

        String newSecretKey=generateSecretKey();

        user.setSecretKey(newSecretKey);
        user.setIsFirstTimeLogin(true);

        userRepository.save(user);

        log.info("2FA secret regenerated successfully for user: {}",email);
    }
}