package com.identity.persistence.model.version;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelVersionRepository extends JpaRepository<ModelVersion, Long> {
    Optional<ModelVersion> findByVersion(String version);

    Optional<ModelVersion> findByActiveTrue();
}
