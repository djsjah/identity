package com.identity.persistence.verification.history;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

public interface VerificationHistoryRepository extends JpaRepository<VerificationHistory, String> {
    @Override
    @NonNull
    @EntityGraph(VerificationHistory.WITH_RELATIONS)
    Optional<VerificationHistory> findById(@NonNull String id);

    @EntityGraph(VerificationHistory.WITH_RELATIONS)
    @Query("""
            SELECT h
            FROM VerificationHistory h
            """)
    Slice<VerificationHistory> findSlice(Pageable pageable);

    @EntityGraph(VerificationHistory.WITH_RELATIONS)
    Slice<VerificationHistory> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(VerificationHistory.WITH_RELATIONS)
    Slice<VerificationHistory> findByImageId(String imageId, Pageable pageable);

    @EntityGraph(VerificationHistory.WITH_RELATIONS)
    Slice<VerificationHistory> findByVerificationDateBetween(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    @EntityGraph(VerificationHistory.WITH_RELATIONS)
    Slice<VerificationHistory> findByResult(String result, Pageable pageable);
}
