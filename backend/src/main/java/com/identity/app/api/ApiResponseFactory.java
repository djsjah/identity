package com.identity.app.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

import com.identity.app.api.dto.ResponseWrapperDTO;

@Component
@RequiredArgsConstructor
public class ApiResponseFactory {
    private final ApiResponseProperties properties;

    public <T> ResponseWrapperDTO<T> success(T data) {
        return new ResponseWrapperDTO<>(HttpStatus.OK.value(), properties.successMessage(), data);
    }

    public ResponseWrapperDTO<Void> error(HttpStatusCode status, String message) {
        return new ResponseWrapperDTO<>(status.value(), message, null);
    }
}
