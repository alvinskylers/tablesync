package com.alvinskylers.tablesync.service;

import com.alvinskylers.tablesync.dto.user.UserRequest;
import com.alvinskylers.tablesync.dto.user.UserResponse;
import com.alvinskylers.tablesync.entity.User;
import com.alvinskylers.tablesync.exception.EmailAlreadyExistsException;
import com.alvinskylers.tablesync.exception.UserNotFoundException;
import com.alvinskylers.tablesync.mapper.UserMapper;
import com.alvinskylers.tablesync.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public Page<UserResponse> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::mapUserToUserResponse);
    }

    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("this email has been taken");
        }

        User user = User.builder()
                .email(request.email())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        userRepository.save(user);
        return userMapper.mapUserToUserResponse(user);
    }

    public UserResponse viewUser(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(()-> new UserNotFoundException("User not found with id: " + userId));
        return userMapper.mapUserToUserResponse(user);
    }

    public UserResponse updateUser(UUID userId, UserRequest request) {

        var user = userRepository.findById(userId)
                .orElseThrow(()-> new UserNotFoundException("User not found with id: " + userId));

        if (!user.getEmail().equals(request.email())
                && userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("This email is already taken.");
        }

        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setRole(request.role());
        userRepository.save(user);
        return userMapper.mapUserToUserResponse(user);
    }

    public void deleteUser(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(()-> new UserNotFoundException("User not found with id: " + userId));
        userRepository.delete(user);
    }

}
