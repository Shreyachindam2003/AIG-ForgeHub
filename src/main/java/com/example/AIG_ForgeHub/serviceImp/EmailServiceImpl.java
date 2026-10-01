package com.example.AIG_ForgeHub.serviceImp;

import com.example.AIG_ForgeHub.model.User;
import com.example.AIG_ForgeHub.repository.UserRepository;
import com.example.AIG_ForgeHub.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final UserRepository userRepository;

    private final JavaMailSender javaMailSender;

    @Override
    public void sendOtp(String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Email not registered"
                                )
                        );

        String otp =
                String.format(
                        "%06d",
                        new Random().nextInt(1000000)
                );

        user.setEmailOtpHash(otp);

        user.setEmailOtpExpiry(
                LocalDateTime.now().plusMinutes(5)
        );

        userRepository.save(user);

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(email);

        message.setSubject(
                "AIG ForgeHub - OTP Verification"
        );

        message.setText(
                "Your AIG ForgeHub OTP is: "
                        + otp
                        + "\n\n"
                        + "This OTP is valid for 5 minutes."
        );

        javaMailSender.send(message);
    }

    @Override
    public boolean verifyOtp(
            String email,
            String otp) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Email not registered"
                                )
                        );

        if (user.getEmailOtpHash() == null ||
                user.getEmailOtpExpiry() == null) {

            return false;
        }

        if (user.getEmailOtpExpiry()
                .isBefore(LocalDateTime.now())) {

            return false;
        }

        if (!user.getEmailOtpHash().equals(otp)) {

            return false;
        }

        user.setEmailOtpHash(null);
        user.setEmailOtpExpiry(null);

        userRepository.save(user);

        return true;
    }
}