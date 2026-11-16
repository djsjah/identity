package com.identity.persistence.verification.task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.identity.core.image.task.lifecycle.ImageVerificationFailure;
import com.identity.core.image.task.lifecycle.ImageVerificationTaskStatus;

public interface VerificationTaskRepository extends JpaRepository<VerificationTask, String> {
    Optional<VerificationTask> findByImageIdAndUserId(String imageId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT t
            FROM VerificationTask t
            WHERE t.imageId = :imageId
            """)
    Optional<VerificationTask> findForUpdate(@Param("imageId") String imageId);

    @Query("""
            SELECT t.imageId FROM VerificationTask t
            WHERE t.status = :pending AND t.nextAttemptAt <= :now
              AND (t.lastDispatchedAt IS NULL OR t.lastDispatchedAt < :cutoff)
            ORDER BY t.createdAt, t.imageId
            """)
    List<String> findReadyToDispatch(
            @Param("pending") ImageVerificationTaskStatus pending,
            @Param("now") LocalDateTime now,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );

    @Modifying
    @Query("""
            UPDATE VerificationTask t SET t.lastDispatchedAt = :now
            WHERE t.imageId = :imageId AND t.status = :pending
              AND t.nextAttemptAt <= :now
              AND (t.lastDispatchedAt IS NULL OR t.lastDispatchedAt < :cutoff)
            """)
    int reserveDispatch(
            @Param("imageId") String imageId,
            @Param("pending") ImageVerificationTaskStatus pending,
            @Param("now") LocalDateTime now,
            @Param("cutoff") LocalDateTime cutoff
    );

    @Modifying
    @Query("""
            UPDATE VerificationTask t
            SET t.status = :failed, t.errorCode = :timeout, t.finishedAt = :now
            WHERE t.status = :processing AND t.startedAt < :cutoff AND t.attempts >= :maxAttempts
            """)
    void failStaleTasks(
            @Param("maxAttempts") int maxAttempts,
            @Param("processing") ImageVerificationTaskStatus processing,
            @Param("failed") ImageVerificationTaskStatus failed,
            @Param("timeout") ImageVerificationFailure timeout,
            @Param("now") LocalDateTime now,
            @Param("cutoff") LocalDateTime cutoff
    );

    @Modifying
    @Query("""
            UPDATE VerificationTask t
            SET t.status = :pending, t.startedAt = NULL, t.nextAttemptAt = :now, t.lastDispatchedAt = NULL
            WHERE t.status = :processing AND t.startedAt < :cutoff AND t.attempts < :maxAttempts
            """)
    void recoverStaleTasks(
            @Param("maxAttempts") int maxAttempts,
            @Param("processing") ImageVerificationTaskStatus processing,
            @Param("pending") ImageVerificationTaskStatus pending,
            @Param("now") LocalDateTime now,
            @Param("cutoff") LocalDateTime cutoff
    );
}
