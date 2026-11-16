package com.identity.core.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.identity.core.auth.validation.PasswordConstraints;
import com.identity.core.user.UserConstraints;

public record SignInRequestDTO(
        @NotBlank(message = "Username cannot be blank")
        @Size(
                min = UserConstraints.USERNAME_MIN_LENGTH,
                max = UserConstraints.USERNAME_MAX_LENGTH,
                message = "Username must be between {min} and {max} characters"
        )
        String username,

        @NotBlank(message = "Password cannot be blank")
        @Size(
                max = PasswordConstraints.MAX_BCRYPT_BYTES,
                message = "Password length must not exceed {max} characters"
        )
        String password,

        Boolean rememberMe
) { }
