package com.identity.core.auth.dto;

import jakarta.validation.constraints.NotBlank;

import com.identity.core.auth.validation.ValidPassword;

public record ResetPasswordRequestDTO(
        @NotBlank(message = "Username cannot be blank")
        String username,

        @ValidPassword
        String newPassword,

        @NotBlank(message = "Confirmation code cannot be blank")
        String confirmationCode
) { }
