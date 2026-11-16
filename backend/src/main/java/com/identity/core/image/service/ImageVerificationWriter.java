package com.identity.core.image.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.identity.infrastructure.ml.dto.PredictionResponseDTO;
import com.identity.persistence.verification.history.VerificationHistory;
import com.identity.persistence.verification.history.VerificationHistoryRepository;
import com.identity.persistence.image.ImageRepository;
import com.identity.persistence.user.UserRepository;

@Service
@RequiredArgsConstructor
public class ImageVerificationWriter {
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final VerificationHistoryRepository historyRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void create(
            long userId,
            String imageId,
            LocalDateTime verificationDate,
            PredictionResponseDTO prediction
    ) {
        historyRepository.save(VerificationHistory.builder()
                .id(UUID.randomUUID().toString())
                .image(imageRepository.getReferenceById(imageId))
                .user(userRepository.getReferenceById(userId))
                .verificationDate(verificationDate)
                .result(
                        String.format(
                                Locale.ROOT,
                                "class_description: %s, fake_probability: %s",
                                prediction.classDescription(),
                                prediction.fakeProbability()
                        )
                )
                .build()
        );
    }
}
