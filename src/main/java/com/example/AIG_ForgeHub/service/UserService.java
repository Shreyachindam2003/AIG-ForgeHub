package com.example.AIG_ForgeHub.service;


import com.example.AIG_ForgeHub.dto.dashboardDto.UserRequestDto;
import com.example.AIG_ForgeHub.dto.dashboardDto.UserResponseDto;

import java.util.List;

public interface UserService {

    UserResponseDto addUser(UserRequestDto userRequestDto);

    UserResponseDto getUserById(Long id);

    UserResponseDto getUserByEmail(String email);

    UserResponseDto createVendor(UserRequestDto request);

    List<UserResponseDto> getAllUsers();
}