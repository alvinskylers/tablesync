package com.alvinskylers.tablesync.dto.user;

import com.alvinskylers.tablesync.entity.enums.Role;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String email,
        String phone,
        Role role
) {
}
