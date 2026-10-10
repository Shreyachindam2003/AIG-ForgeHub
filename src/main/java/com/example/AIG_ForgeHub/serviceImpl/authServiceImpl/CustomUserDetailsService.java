package com.example.AIG_ForgeHub.serviceImpl.authServiceImpl;

import com.example.AIG_ForgeHub.entity.User;
import com.example.AIG_ForgeHub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email)throws UsernameNotFoundException {

        log.info("Loading user details for authentication: {}",email);

        User user=userRepository.findByEmail(email)
                .orElseThrow(()->{
                    log.warn("User not found during authentication: {}",email);
                    return new UsernameNotFoundException("User not found");
                });

        log.debug("User details loaded successfully for authentication: {}",email);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole())
                .build();
    }
}