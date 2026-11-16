package com.identity.core.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.identity.app.api.ApiResponseFactory;
import com.identity.app.api.dto.ResponseWrapperDTO;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AuthController.class)
@RequiredArgsConstructor
public class AuthExceptionHandler {
    private final ApiResponseFactory apiResponseFactory;

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ResponseWrapperDTO<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(apiResponseFactory.error(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    }
}
