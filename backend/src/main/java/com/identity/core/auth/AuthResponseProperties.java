package com.identity.core.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.response")
public record AuthResponseProperties(String logoutMessage) { }
