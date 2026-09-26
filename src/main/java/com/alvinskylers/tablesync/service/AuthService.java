package com.alvinskylers.tablesync.service;

import com.alvinskylers.tablesync.dto.auth.AuthResponse;
import com.alvinskylers.tablesync.dto.auth.LoginRequest;
import com.alvinskylers.tablesync.dto.auth.RegisterRequest;
import com.alvinskylers.tablesync.dto.user.UserResponse;
import com.alvinskylers.tablesync.entity.User;
import com.alvinskylers.tablesync.entity.enums.Role;
import com.alvinskylers.tablesync.exception.EmailAlreadyExistsException;
import com.alvinskylers.tablesync.mapper.UserMapper;
import com.alvinskylers.tablesync.repository.UserRepository;
import com.alvinskylers.tablesync.security.JwtService;
import com.alvinskylers.tablesync.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

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

    public AuthResponse login(LoginRequest request) {
        Authentication authResult = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        UserPrincipal userPrincipal = (UserPrincipal) authResult.getPrincipal();
        String token = jwtService.generateToken(userPrincipal);
        return  new AuthResponse(token);
    }
}
