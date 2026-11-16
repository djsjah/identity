package com.identity.core.image.dto;

import com.identity.core.image.task.lifecycle.ImageVerificationTaskStatus;

public record ImageUploadResponseDTO(String imageId, ImageVerificationTaskStatus status) { }
