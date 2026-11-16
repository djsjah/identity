package com.identity.core.image.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.identity.app.api.ApiResponseFactory;
import com.identity.app.api.dto.ResponseWrapperDTO;
import com.identity.core.image.ImageUploadController;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ImageUploadController.class)
@RequiredArgsConstructor
public class ImageUploadExceptionHandler {
    private final ApiResponseFactory apiResponseFactory;

    @ExceptionHandler(ImageStorageException.class)
    public ResponseEntity<ResponseWrapperDTO<Void>> handleStorageFailure(ImageStorageException ex) {
        log.error("Image storage operation failed", ex);

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(apiResponseFactory.error(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Image storage is temporarily unavailable"
                ));
    }
}
