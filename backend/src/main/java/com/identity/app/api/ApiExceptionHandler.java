package com.identity.app.api;

import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.identity.app.api.dto.ResponseWrapperDTO;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private final ApiResponseFactory apiResponseFactory;

    @Override
    @Nullable
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));

        return handleExceptionInternal(
                ex,
                apiResponseFactory.error(status, message.isBlank() ? "Invalid request data" : message),
                headers,
                status,
                request
        );
    }

    @Override
    @Nullable
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            @NonNull HandlerMethodValidationException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        if (status.is5xxServerError()) {
            return handleExceptionInternal(
                    ex,
                    apiResponseFactory.error(status, "Internal server error"),
                    headers,
                    status,
                    request
            );
        }

        String message = ex.getParameterValidationResults()
                .stream()
                .flatMap(result -> {
                    String parameterName = result.getMethodParameter().getParameterName();

                    return result.getResolvableErrors()
                            .stream()
                            .map(error -> (parameterName != null
                                    ? parameterName
                                    : "parameter #" + result.getMethodParameter().getParameterIndex())
                                    + ": " + error.getDefaultMessage()
                            );
                })
                .distinct()
                .collect(Collectors.joining("; "));

        return handleExceptionInternal(
                ex,
                apiResponseFactory.error(status, message.isBlank() ? "Invalid request parameters" : message),
                headers,
                status,
                request
        );
    }

    @Override
    @Nullable
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            @NonNull MaxUploadSizeExceededException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        return handleExceptionInternal(
                ex,
                apiResponseFactory.error(status, "Uploaded file exceeds the allowed size"),
                headers,
                status,
                request
        );
    }

    @Override
    @NonNull
    protected ResponseEntity<Object> createResponseEntity(
            @Nullable Object body,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        if (body instanceof ResponseWrapperDTO<?>) return new ResponseEntity<>(body, headers, status);

        String message;
        if (status.value() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            message = "Internal server error";
        }
        else if (body instanceof ProblemDetail problem && problem.getDetail() != null && !problem.getDetail().isBlank()) {
            message = problem.getDetail();
        }
        else {
            HttpStatus httpStatus = HttpStatus.resolve(status.value());
            message = httpStatus == null
                    ? "Request failed"
                    : httpStatus.getReasonPhrase();
        }

        return new ResponseEntity<>(apiResponseFactory.error(status, message), headers, status);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseWrapperDTO<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                String constraint = violation.getConstraintName();

                if ("uk_users_username".equals(constraint)) {
                    return errorResponse(HttpStatus.CONFLICT, "Username already exists");
                }
                if ("uk_users_email".equals(constraint) || "uk_users_email_upper".equals(constraint)) {
                    return errorResponse(HttpStatus.CONFLICT, "Email already exists");
                }
            }

            cause = cause.getCause();
        }

        log.error("Database integrity violation", ex);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseWrapperDTO<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected application error", ex);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private ResponseEntity<ResponseWrapperDTO<Void>> errorResponse(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(apiResponseFactory.error(status, message));
    }
}
