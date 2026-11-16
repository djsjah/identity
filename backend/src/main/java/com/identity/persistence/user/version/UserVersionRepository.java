package com.identity.persistence.user.version;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserVersionRepository extends JpaRepository<UserVersion, Long> {
    @Query("""
        SELECT v
        FROM UserVersion v
        WHERE v.user.id = :userId
          AND v.validTo IS NULL
        """)
    Optional<UserVersion> findCurrentByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT v
            FROM UserVersion v
            WHERE v.user.id = :userId
              AND v.validFrom <= :timestamp
              AND (v.validTo IS NULL OR v.validTo > :timestamp)
            """)
    Optional<UserVersion> findAt(@Param("userId") Long userId, @Param("timestamp") LocalDateTime timestamp);
}
