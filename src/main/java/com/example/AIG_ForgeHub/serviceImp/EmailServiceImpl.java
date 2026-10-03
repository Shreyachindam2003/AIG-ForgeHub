package com.example.AIG_ForgeHub.serviceImp;

import com.example.AIG_ForgeHub.model.User;
import com.example.AIG_ForgeHub.repository.UserRepository;
import com.example.AIG_ForgeHub.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final UserRepository userRepository;
    private final JavaMailSender javaMailSender;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void sendOtp(String email) {

        log.info("OTP generation requested for user: {}",email);

        User user=userRepository.findByEmail(email)
                .orElseThrow(()->{
                    log.warn("OTP requested for unregistered email: {}",email);
                    return new RuntimeException("Email not registered");
                });

        String otp=String.format("%06d",new Random().nextInt(1000000));
        String otpHash=passwordEncoder.encode(otp);

        user.setEmailOtpHash(otpHash);
        user.setEmailOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        SimpleMailMessage message=new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("AIG ForgeHub - OTP Verification");
        message.setText(
                "Hello "+user.getFullName()+",\n\n"
                        +"Your AIG ForgeHub OTP is: "+otp+"\n\n"
                        +"This OTP is valid for 5 minutes.\n\n"
                        +"If you did not request this OTP, please ignore this email."
        );

        try {

            javaMailSender.send(message);

            log.info("OTP email sent successfully to user: {}",email);

        } catch (Exception e) {

            log.error("Failed to send OTP email to user: {}",email,e);

            throw new RuntimeException("Unable to send OTP email",e);
        }
    }

    @Override
    public boolean verifyOtp(String email,String otp) {

        log.info("OTP verification requested for user: {}",email);

        User user=userRepository.findByEmail(email)
                .orElseThrow(()->{
                    log.warn("OTP verification attempted for unregistered email: {}",email);
                    return new RuntimeException("Email not registered");
                });

        if (user.getEmailOtpHash()==null || user.getEmailOtpExpiry()==null) {

            log.warn("OTP not available for user: {}",email);

            return false;
        }

        if (user.getEmailOtpExpiry().isBefore(LocalDateTime.now())) {

            log.warn("OTP expired for user: {}",email);

            return false;
        }

        boolean valid=passwordEncoder.matches(otp,user.getEmailOtpHash());

        if (!valid) {

            log.warn("Invalid OTP entered for user: {}",email);

            return false;
        }

        user.setEmailOtpHash(null);
        user.setEmailOtpExpiry(null);
        userRepository.save(user);

        log.info("OTP verified successfully for user: {}",email);

        return true;
    }
}