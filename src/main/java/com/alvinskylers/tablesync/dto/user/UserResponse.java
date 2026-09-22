package com.alvinskylers.tablesync.dto.user;

import com.alvinskylers.tablesync.entity.enums.Role;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String phone,
        Role role
) {
}
