package com.identity.persistence.auth.session;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.identity.persistence.user.User;

@Entity
@Table(name = "auth_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthSession {
    @Id
    @Column(
            name = "token_hash",
            nullable = false,
            length = 64,
            updatable = false
    )
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder
    private AuthSession(
            String tokenHash,
            User user,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.user = Objects.requireNonNull(user);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
    }
}
