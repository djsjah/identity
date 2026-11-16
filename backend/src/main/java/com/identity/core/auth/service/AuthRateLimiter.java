package com.identity.core.auth.service;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.identity.security.config.SecurityProperties;

@Component
public class AuthRateLimiter {
    private final SecurityProperties.RateLimit rateLimit;
    private final Cache<String, AtomicInteger> attempts;

    public AuthRateLimiter(SecurityProperties properties) {
        rateLimit = properties.rateLimit();
        attempts = Caffeine.newBuilder()
                .maximumSize(rateLimit.maxEntries())
                .expireAfterWrite(rateLimit.window())
                .build();
    }

    public void signIn(HttpServletRequest request, String username) {
        check(rateLimit.requestsPerIp(), "login-ip:" + request.getRemoteAddr());
        check(rateLimit.loginAttempts(), "login-user:" + username.toLowerCase(Locale.ROOT));
    }

    public void signUp(HttpServletRequest request) {
        check(rateLimit.requestsPerIp(), "signup-ip:" + request.getRemoteAddr());
    }

    public void resetPassword(Long userId) { check(rateLimit.resetAttempts(), "password-reset:" + userId); }

    private void check(int maxAttempts, String rateLimitKey) {
        int currentAttemptCount = attempts.asMap()
                .computeIfAbsent(rateLimitKey, ignored -> new AtomicInteger())
                .incrementAndGet();

        if (currentAttemptCount > maxAttempts) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later");
        }
    }
}
