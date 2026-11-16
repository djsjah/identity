package com.identity.core.auth.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.identity.persistence.auth.session.AuthSession;
import com.identity.persistence.auth.session.AuthSessionRepository;
import com.identity.persistence.user.User;
import com.identity.security.config.SecurityProperties;

@Service
@RequiredArgsConstructor
public class SessionService {
    private static final int TOKEN_BYTES = 32;
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final AuthSessionRepository sessionRepository;
    private final SecurityProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public SessionTicket create(boolean rememberMe, User user, String previousToken) {
        revoke(previousToken);

        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);

        Duration sessionTtl = rememberMe
                ? properties.session().rememberMeTtl()
                : properties.session().ttl();

        Instant createdAt = Instant.now();

        sessionRepository.save(AuthSession.builder()
                .tokenHash(hash(token))
                .user(user)
                .createdAt(createdAt)
                .expiresAt(createdAt.plus(sessionTtl))
                .build()
        );
        return new SessionTicket(rememberMe, user, token, sessionTtl);
    }

    @Transactional(readOnly = true)
    public Optional<User> findUser(String token) {
        if (!isValidFormat(token)) return Optional.empty();

        return sessionRepository.findWithUser(hash(token))
                .filter(session -> session.getExpiresAt().isAfter(Instant.now()))
                .map(AuthSession::getUser);
    }

    @Transactional
    public void revokeAll(Long userId) { sessionRepository.deleteAllByUserId(userId); }

    @Transactional
    public void revoke(String token) { if (isValidFormat(token)) sessionRepository.deleteByTokenHash(hash(token)); }

    @Transactional
    public void deleteExpired() { sessionRepository.deleteExpired(Instant.now()); }

    private static boolean isValidFormat(String token) {
        return token != null && TOKEN_PATTERN.matcher(token).matches();
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII))
            );
        }
        catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
