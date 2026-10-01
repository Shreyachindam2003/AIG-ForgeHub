package com.example.AIG_ForgeHub.serviceImp;

import com.example.AIG_ForgeHub.model.User;
import com.example.AIG_ForgeHub.repository.UserRepository;
import com.example.AIG_ForgeHub.service.TwoFactorService;
import com.example.AIG_ForgeHub.util.QrCodeUtil;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(
                        () -> new RuntimeException(
                                "User not found"
                        )
                );

        String secretKey = user.getSecretKey();

        String issuer = "AIG ForgeHub";

        String qrText =
                "otpauth://totp/"
                        + issuer
                        + ":"
                        + email
                        + "?secret="
                        + secretKey
                        + "&issuer="
                        + issuer;

        return qrCodeUtil.generateQrCode(qrText);
    }

    @Override
    public boolean verifyOtp(String email, int otp) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(
                        () -> new RuntimeException(
                                "User not found"
                        )
                );

        if (user.getSecretKey() == null) {
            return false;
        }

        GoogleAuthenticator googleAuthenticator =
                new GoogleAuthenticator();

        return googleAuthenticator.authorize(
                user.getSecretKey(),
                otp
        );
    }

    @Override
    public void regenerateSecretKey(String email) {

        User user = userRepository
                .findByEmail(email)
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
    }
}
