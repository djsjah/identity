package com.identity.core.image.task.dto;

import com.identity.core.image.task.lifecycle.ImageVerificationFailure;
import com.identity.core.image.task.lifecycle.ImageVerificationTaskStatus;

public record ImageVerificationTaskStatusDTO(
        String imageId,
        ImageVerificationTaskStatus status,
        Integer predictedClass,
        String classDescription,
        Double fakeProbability,
        ImageVerificationFailure error
) { }
