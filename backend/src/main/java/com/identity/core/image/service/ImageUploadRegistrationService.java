package com.identity.core.image.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.identity.core.image.task.lifecycle.ImageVerificationTaskStatus;
import com.identity.persistence.image.Image;
import com.identity.persistence.image.ImageRepository;
import com.identity.persistence.verification.task.VerificationTask;
import com.identity.persistence.verification.task.VerificationTaskRepository;

@Service
@RequiredArgsConstructor
public class ImageUploadRegistrationService {
    private final ImageRepository imageRepository;
    private final VerificationTaskRepository taskRepository;

    @Transactional
    public void register(long userId, Image image, String contentType) {
        imageRepository.save(image);
        taskRepository.save(VerificationTask.builder()
                .imageId(image.getId())
                .userId(userId)
                .contentType(contentType)
                .status(ImageVerificationTaskStatus.PENDING)
                .attempts(0)
                .createdAt(image.getUploadDate())
                .nextAttemptAt(image.getUploadDate())
                .build()
        );
    }
}
