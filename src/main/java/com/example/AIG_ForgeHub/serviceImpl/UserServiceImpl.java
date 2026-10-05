package com.example.AIG_ForgeHub.serviceImpl;


import com.example.AIG_ForgeHub.dto.dashboardDto.UserRequestDto;
import com.example.AIG_ForgeHub.dto.dashboardDto.UserResponseDto;
import com.example.AIG_ForgeHub.entity.User;
import com.example.AIG_ForgeHub.exception.BusinessException;
import com.example.AIG_ForgeHub.repository.UserRepository;
import com.example.AIG_ForgeHub.service.UserService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponseDto addUser(UserRequestDto request) {
        if(userRepository.existsByEmail(request.getEmail())){
            throw new BusinessException("A user with this email already exists. Please use a different email address.");
        }

        User user=modelMapper.map(request,User.class);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole()==null?"VENDOR":request.getRole());
        user.setIsFirstTimeLogin(true);
        user.setSecretKey(null);
        user.setRefreshTokenHash(null);
        user.setRefreshTokenRevoked(false);

        return modelMapper.map(userRepository.save(user),UserResponseDto.class);
    }

    @Override
    @Transactional(readOnly=true)
    public UserResponseDto getUserById(Long id) {
        User user=userRepository.findById(id)
                .orElseThrow(()->new BusinessException("User not found with ID: "+id));

        return modelMapper.map(user,UserResponseDto.class);
    }

    @Override
    @Transactional(readOnly=true)
    public UserResponseDto getUserByEmail(String email) {
        User user=userRepository.findByEmail(email)
                .orElseThrow(()->new BusinessException("User not found with email: "+email));

        return modelMapper.map(user,UserResponseDto.class);
    }

    @Override
    @Transactional
    public UserResponseDto createVendor(UserRequestDto request) {
        if(userRepository.existsByEmail(request.getEmail())){
            throw new BusinessException("A user with this email already exists. Please use a different email address.");
        }

        User user=modelMapper.map(request,User.class);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("VENDOR");
        user.setIsFirstTimeLogin(true);
        user.setSecretKey(null);
        user.setRefreshTokenHash(null);
        user.setRefreshTokenRevoked(false);

        return modelMapper.map(userRepository.save(user),UserResponseDto.class);
    }

    @Override
    @Transactional(readOnly=true)
    public List<UserResponseDto> getAllUsers() {
        List<User> users=userRepository.findAll();
        List<UserResponseDto> response=new ArrayList<>();

        for(User user:users){
            response.add(modelMapper.map(user,UserResponseDto.class));
        }

        return response;
    }
}