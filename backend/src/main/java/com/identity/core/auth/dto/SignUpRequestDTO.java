package com.identity.core.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.identity.core.auth.validation.ValidPassword;
import com.identity.core.user.UserConstraints;

public record SignUpRequestDTO(
        @NotBlank(message = "Username cannot be blank")
        @Size(
                min = UserConstraints.USERNAME_MIN_LENGTH,
                max = UserConstraints.USERNAME_MAX_LENGTH,
                message = "Username must be between {min} and {max} characters"
        )
        String username,

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Invalid email address")
        @Size(
                max = UserConstraints.EMAIL_MAX_LENGTH,
                message = "Email address must not exceed {max} characters"
        )
        String email,

        @ValidPassword
        String password
) { }
