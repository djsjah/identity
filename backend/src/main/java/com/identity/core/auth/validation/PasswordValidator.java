package com.identity.core.auth.validation;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) return false;
        if (password.codePointCount(0, password.length()) < PasswordConstraints.MIN_LENGTH) return false;
        return password.getBytes(StandardCharsets.UTF_8).length <= PasswordConstraints.MAX_BCRYPT_BYTES;
    }
}
