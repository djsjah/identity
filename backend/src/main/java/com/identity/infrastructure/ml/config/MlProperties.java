package com.identity.infrastructure.ml.config;

import java.net.URI;
import java.time.Duration;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.ml")
public record MlProperties(
        @NotNull URI predictUrl,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout
) { }
