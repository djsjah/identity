package com.identity.persistence.auth.session;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {
    @Query("""
            SELECT s
            FROM AuthSession s
            JOIN FETCH s.user
            WHERE s.tokenHash = :tokenHash
            """)
    Optional<AuthSession> findWithUser(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("""
            DELETE FROM AuthSession s
            WHERE s.tokenHash = :tokenHash
            """)
    int deleteByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("""
            DELETE FROM AuthSession s
            WHERE s.user.id = :userId
            """)
    int deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("""
            DELETE FROM AuthSession s
            WHERE s.expiresAt <= :now
            """)
    int deleteExpired(@Param("now") Instant now);
}
