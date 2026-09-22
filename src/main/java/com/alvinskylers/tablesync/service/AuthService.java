package com.alvinskylers.tablesync.service;

import com.alvinskylers.tablesync.dto.auth.RegisterRequest;
import com.alvinskylers.tablesync.dto.user.UserResponse;
import com.alvinskylers.tablesync.entity.User;
import com.alvinskylers.tablesync.entity.enums.Role;
import com.alvinskylers.tablesync.exception.EmailAlreadyExistsException;
import com.alvinskylers.tablesync.mapper.UserMapper;
import com.alvinskylers.tablesync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("this email has been taken");
        }

        User user = User.builder()
                    .email(request.email())
                    .passwordHash(passwordEncoder.encode(request.password()))
                    .phone(request.phone())
                    .role(Role.CUSTOMER)
                    .build();

        userRepository.save(user);
        return userMapper.mapUserToUserResponse(user);

    }
}
