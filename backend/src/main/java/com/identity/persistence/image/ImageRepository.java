package com.identity.persistence.image;

import java.time.LocalDateTime;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ImageRepository extends JpaRepository<Image, String> {
    @Query("""
            SELECT i
            FROM Image i
            """)
    Slice<Image> findSlice(Pageable pageable);

    Slice<Image> findByUploadDateBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Slice<Image> findByFilepathContainingIgnoreCase(String partialPath, Pageable pageable);
}
