package com.identity.core.image.task.dto;

public record ImageVerificationTaskClaimDTO(
        int attempt,
        long userId,
        String imageId,
        String objectKey,
        String contentType
) { }
