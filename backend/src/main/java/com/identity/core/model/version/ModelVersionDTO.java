package com.identity.core.model.version;

import java.time.LocalDateTime;

public record ModelVersionDTO(
        boolean active,
        Long id,
        String version,
        String featureVersion,
        String datasetVersion,
        LocalDateTime createdAt
) { }
