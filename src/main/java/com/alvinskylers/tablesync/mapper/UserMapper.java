package com.alvinskylers.tablesync.mapper;

import com.alvinskylers.tablesync.dto.user.UserResponse;
import com.alvinskylers.tablesync.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse mapUserToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .build();
    }

}
