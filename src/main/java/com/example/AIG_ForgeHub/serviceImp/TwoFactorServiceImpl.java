package com.example.AIG_ForgeHub.serviceImp;

import com.example.AIG_ForgeHub.model.User;
import com.example.AIG_ForgeHub.repository.UserRepository;
import com.example.AIG_ForgeHub.service.TwoFactorService;
import com.example.AIG_ForgeHub.util.QrCodeUtil;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class TwoFactorServiceImpl implements TwoFactorService {

    private final UserRepository userRepository;
    private final QrCodeUtil qrCodeUtil;

    @Override
    public String generateSecretKey() {

        GoogleAuthenticator googleAuthenticator =
                new GoogleAuthenticator();

        GoogleAuthenticatorKey key =
                googleAuthenticator.createCredentials();

        return key.getKey();
    }

    @Override
    public String generateQrCode(String email) {

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "User not found"
                                )
                        );

        String secretKey =
                user.getSecretKey();

        if (secretKey == null ||
                secretKey.isBlank()) {

            throw new RuntimeException(
                    "Secret key not found"
            );
        }

        String issuer = "AIG ForgeHub";

        String encodedIssuer =
                URLEncoder.encode(
                        issuer,
                        StandardCharsets.UTF_8
                );

        String encodedEmail =
                URLEncoder.encode(
                        email,
                        StandardCharsets.UTF_8
                );

        String qrText =
                "otpauth://totp/"
                        + encodedIssuer
                        + ":"
                        + encodedEmail
                        + "?secret="
                        + secretKey
                        + "&issuer="
                        + encodedIssuer;

        System.out.println(
                "========== GENERATE QR START =========="
        );

        System.out.println(
                "EMAIL = " + email
        );

        System.out.println(
                "SECRET KEY EXISTS = "
                        + (secretKey != null)
        );

        System.out.println(
                "QR GENERATED SUCCESSFULLY"
        );

        System.out.println(
                "========== GENERATE QR END =========="
        );

        return qrCodeUtil.generateQrCode(qrText);
    }

    @Override
    public boolean verifyOtp(
            String email,
            int otp) {

        System.out.println(
                "========== VERIFY GOOGLE OTP START =========="
        );

        System.out.println(
                "EMAIL = " + email
        );

        System.out.println(
                "OTP ENTERED = " + otp
        );

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "User not found"
                                )
                        );

        String secretKey =
                user.getSecretKey();

        if (secretKey == null ||
                secretKey.isBlank()) {

            System.out.println(
                    "SECRET KEY NOT FOUND"
            );

            return false;
        }

        System.out.println(
                "SECRET KEY EXISTS = true"
        );

        GoogleAuthenticator googleAuthenticator =
                new GoogleAuthenticator();

        boolean valid =
                googleAuthenticator.authorize(
                        secretKey,
                        otp
                );

        System.out.println(
                "GOOGLE OTP VALID = " + valid
        );

        System.out.println(
                "========== VERIFY GOOGLE OTP END =========="
        );

        return valid;
    }

    @Override
    public void regenerateSecretKey(
            String email) {

        System.out.println(
                "========== REGENERATE SECRET START =========="
        );

        System.out.println(
                "EMAIL = " + email
        );

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "User not found"
                                )
                        );

        String newSecretKey =
                generateSecretKey();

        user.setSecretKey(newSecretKey);

        user.setIsFirstTimeLogin(true);

        userRepository.save(user);

        System.out.println(
                "NEW SECRET KEY SAVED"
        );

        System.out.println(
                "========== REGENERATE SECRET END =========="
        );
    }
}