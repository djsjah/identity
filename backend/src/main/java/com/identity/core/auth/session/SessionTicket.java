package com.identity.core.auth.session;

import java.time.Duration;

import com.identity.persistence.user.User;

public record SessionTicket(
        boolean rememberMe,
        User user,
        String token,
        Duration ttl
) { }
