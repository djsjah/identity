package com.identity.app.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.api-response")
public record ApiResponseProperties(String successMessage) { }
