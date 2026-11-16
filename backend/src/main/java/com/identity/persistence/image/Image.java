package com.identity.persistence.image;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "image")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image {
    @Id
    @Column(nullable = false)
    private String id;

    @Column(nullable = false, length = 1024)
    private String filepath;

    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;

    @Builder
    private Image(String id, String filepath, LocalDateTime uploadDate) {
        this.id = Objects.requireNonNull(id);
        this.filepath = Objects.requireNonNull(filepath);
        this.uploadDate = Objects.requireNonNull(uploadDate);
    }
}
