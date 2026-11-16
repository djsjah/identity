package com.identity.infrastructure.s3;

import java.net.URI;
import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.storage.s3")
public record S3StorageProperties(
        boolean pathStyleAccess,
        @NotNull URI endpoint,
        @NotBlank String region,
        @NotBlank String bucket,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @NotNull Duration apiCallTimeout,
        @NotNull Duration apiCallAttemptTimeout,
        @NotNull DataSize maxObjectSize
) { }
