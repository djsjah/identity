package com.identity.core.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.identity.core.auth.validation.PasswordConstraints;
import com.identity.core.user.UserConstraints;

public record ChangeEmailRequestDTO(
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Invalid email address")
        @Size(
                max = UserConstraints.EMAIL_MAX_LENGTH,
                message = "Email address must not exceed {max} characters"
        )
        String email,

        @NotBlank(message = "Current password is required")
        @Size(max = PasswordConstraints.MAX_BCRYPT_BYTES)
        String currentPassword
) { }
