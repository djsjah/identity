package com.identity.core.image.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.identity.app.utils.UtcDateTimes;
import com.identity.app.logging.LogExecutionTime;
import com.identity.core.image.validation.ImageUploadData;
import com.identity.core.image.validation.ImageUploadValidator;
import com.identity.core.image.port.ImageStorage;
import com.identity.core.image.task.lifecycle.ImageVerificationTaskStatus;
import com.identity.core.image.dto.ImageUploadResponseDTO;
import com.identity.persistence.image.Image;
import com.identity.persistence.user.User;

@Slf4j
@Service
@LogExecutionTime
@RequiredArgsConstructor
public class ImageVerificationFacade {
    private static final int MAX_FILENAME_LENGTH = 80;
    private static final String DEFAULT_FILENAME = "image";
    private static final String IMAGE_PREFIX = "images/";

    private final ImageUploadValidator uploadValidator;
    private final ImageStorage imageStorage;
    private final ImageUploadRegistrationService uploadRegistrationService;

    public ImageUploadResponseDTO upload(MultipartFile file, User user) {
        ImageUploadData image = uploadValidator.validate(file);

        String imageId = UUID.randomUUID().toString();
        String objectKey = IMAGE_PREFIX + buildFilename(imageId, file.getOriginalFilename(), image.extension());
        imageStorage.save(objectKey, image.contentType(), image.content());

        try {
            uploadRegistrationService.register(
                    user.getId(),
                    Image.builder()
                            .id(imageId)
                            .filepath(objectKey)
                            .uploadDate(UtcDateTimes.nowMicros())
                            .build(),
                    image.contentType());
        }
        catch (RuntimeException failure) {
            cleanupFailedUpload(imageId, objectKey, failure);
            throw failure;
        }

        return new ImageUploadResponseDTO(imageId, ImageVerificationTaskStatus.PENDING);
    }

    private void cleanupFailedUpload(String imageId, String objectKey, RuntimeException failure) {
        try {
            imageStorage.delete(objectKey);
        }
        catch (RuntimeException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
            log.error("Failed to clean up image after database failure: imageId={}", imageId, cleanupFailure);
        }
    }

    private static String buildFilename(String imageId, String originalFilename, String extension) {
        String name = originalFilename == null
                ? ""
                : originalFilename.replace('\\', '/');

        name = name.substring(name.lastIndexOf('/') + 1);

        int extensionIndex = name.lastIndexOf('.');
        if (extensionIndex >= 0) name = name.substring(0, extensionIndex);

        String safeName = sanitizeFilename(name);
        if (safeName.isEmpty()) safeName = DEFAULT_FILENAME;

        return imageId + "_" + safeName + extension;
    }

    private static String sanitizeFilename(String name) {
        return name.substring(0, Math.min(name.length(), MAX_FILENAME_LENGTH))
                .replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
