package com.identity.persistence.model.version;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "model_version")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModelVersion {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "model_version_id_seq"
    )
    @SequenceGenerator(
            name = "model_version_id_seq",
            sequenceName = "model_version_id_seq",
            allocationSize = 1
    )
    private Long id;

    @Column(nullable = false, unique = true, length = 50, updatable = false)
    private String version;

    @Column(name = "feature_version", nullable = false, length = 50, updatable = false)
    private String featureVersion;

    @Column(name = "dataset_version", nullable = false, length = 100, updatable = false)
    private String datasetVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Builder
    private ModelVersion(
            boolean active,
            String version,
            String featureVersion,
            String datasetVersion,
            LocalDateTime createdAt
    ) {
        this.active = active;
        this.version = Objects.requireNonNull(version);
        this.featureVersion = Objects.requireNonNull(featureVersion);
        this.datasetVersion = Objects.requireNonNull(datasetVersion);
        this.createdAt = Objects.requireNonNull(createdAt);
    }
}
