package com.identity.security.config;

import java.time.Duration;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        @Valid @NotNull Session session,
        @Valid @NotNull RateLimit rateLimit,
        @Valid @NotNull PasswordReset passwordReset,
        @Valid @NotNull Cors cors
) {
    public record Session(
            boolean secureCookie,
            @NotNull Duration ttl,
            @NotNull Duration rememberMeTtl,
            @NotNull Duration cleanupInterval
    ) { }

    public record RateLimit(
            @Min(1) int loginAttempts,
            @Min(1) int requestsPerIp,
            @Min(1) int resetAttempts,
            @Min(1) long maxEntries,
            @NotNull Duration window
    ) { }

    public record PasswordReset(@NotBlank String confirmationCode) { }

    public record Cors(@NotEmpty List<@NotBlank String> allowedOrigins) { }
}
