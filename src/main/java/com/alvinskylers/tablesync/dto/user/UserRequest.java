package com.alvinskylers.tablesync.dto.user;

import com.alvinskylers.tablesync.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(

        @Email
        @NotBlank
        String email,

        String phone,

        @NotBlank
        @Size(min = 8)
        String password,

        @NotNull
        Role role
) {
}
