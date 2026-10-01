package com.example.AIG_ForgeHub.serviceImp;

import com.example.AIG_ForgeHub.model.User;
import com.example.AIG_ForgeHub.repository.UserRepository;
import com.example.AIG_ForgeHub.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final UserRepository userRepository;

    private final JavaMailSender javaMailSender;

    private final PasswordEncoder passwordEncoder;


    @Override
    public void sendOtp(String email) {

        System.out.println(
                "========== SEND EMAIL OTP START =========="
        );

        System.out.println(
                "EMAIL RECEIVED = " + email
        );


        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Email not registered"
                                )
                        );


        System.out.println(
                "USER FOUND = " + user.getEmail()
        );


        String otp =
                String.format(
                        "%06d",
                        new Random().nextInt(1000000)
                );


        System.out.println(
                "OTP GENERATED = " + otp
        );


        String otpHash =
                passwordEncoder.encode(otp);


        user.setEmailOtpHash(otpHash);

        user.setEmailOtpExpiry(
                LocalDateTime.now().plusMinutes(5)
        );

        userRepository.save(user);


        System.out.println(
                "OTP SAVED IN DATABASE"
        );


        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(user.getEmail());

        message.setSubject(
                "AIG ForgeHub - OTP Verification"
        );

        message.setText(
                "Hello " + user.getFullName()
                        + ",\n\n"
                        + "Your AIG ForgeHub OTP is: "
                        + otp
                        + "\n\n"
                        + "This OTP is valid for 5 minutes."
                        + "\n\n"
                        + "If you did not request this OTP, "
                        + "please ignore this email."
        );


        System.out.println(
                "SENDING EMAIL..."
        );


        javaMailSender.send(message);


        System.out.println(
                "OTP EMAIL SENT SUCCESSFULLY"
        );

        System.out.println(
                "========== SEND EMAIL OTP END =========="
        );
    }


    @Override
    public boolean verifyOtp(
            String email,
            String otp) {

        System.out.println(
                "========== VERIFY EMAIL OTP START =========="
        );

        System.out.println(
                "EMAIL = " + email
        );

        System.out.println(
                "OTP ENTERED = " + otp
        );


        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Email not registered"
                                )
                        );

        System.out.println(
                "OTP HASH FROM DB = " + user.getEmailOtpHash()
        );

        System.out.println(
                "OTP EXPIRY FROM DB = " + user.getEmailOtpExpiry()
        );

        System.out.println(
                "CURRENT TIME = " + LocalDateTime.now()
        );


        if (user.getEmailOtpHash() == null ||
                user.getEmailOtpExpiry() == null) {

            System.out.println(
                    "OTP NOT FOUND"
            );

            return false;
        }


        if (user.getEmailOtpExpiry()
                .isBefore(LocalDateTime.now())) {

            System.out.println(
                    "OTP EXPIRED"
            );

            return false;
        }


        boolean valid =
                passwordEncoder.matches(
                        otp,
                        user.getEmailOtpHash()
                );


        if (!valid) {

            System.out.println(
                    "INVALID OTP"
            );

            return false;
        }


        /*
         * OTP successfully verified.
         * OTP ko immediately clear kar rahe hain
         * so that same OTP dobara use na ho.
         */
        user.setEmailOtpHash(null);

        user.setEmailOtpExpiry(null);

        userRepository.save(user);


        System.out.println(
                "EMAIL OTP VERIFIED SUCCESSFULLY"
        );

        System.out.println(
                "========== VERIFY EMAIL OTP END =========="
        );


        return true;
    }
}