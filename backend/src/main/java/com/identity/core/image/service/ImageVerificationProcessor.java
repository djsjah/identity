package com.identity.core.image.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.identity.core.image.exception.ImageStorageException;
import com.identity.core.image.port.ImageStorage;
import com.identity.core.image.task.lifecycle.ImageVerificationFailure;
import com.identity.core.image.task.ImageVerificationTaskService;
import com.identity.core.image.task.dto.ImageVerificationTaskClaimDTO;
import com.identity.infrastructure.ml.NeuralNetworkClient;
import com.identity.infrastructure.ml.dto.PredictionResponseDTO;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageVerificationProcessor {
    private static final String NO_FACE_MESSAGE = "No face detected in the image";

    private final ImageVerificationTaskService taskService;
    private final ImageStorage imageStorage;
    private final NeuralNetworkClient neuralNetworkClient;

    public void process(String imageId) {
        ImageVerificationTaskClaimDTO taskClaim = taskService.claim(imageId);
        if (taskClaim == null) return;

        PredictionResponseDTO prediction;
        try {
            prediction = neuralNetworkClient.sendImageToPrediction(
                    taskClaim.objectKey().substring(taskClaim.objectKey().lastIndexOf('/') + 1),
                    taskClaim.contentType(),
                    imageStorage.read(taskClaim.objectKey())
            );
        }
        catch (RuntimeException ex) {
            ImageVerificationFailure verificationFailure = classifyFailure(ex);
            taskService.fail(taskClaim, verificationFailure);
            log.warn("Image verification failed: imageId={}, failure={}", imageId, verificationFailure);

            return;
        }

        taskService.complete(taskClaim, prediction);
        log.info("Image verification completed: imageId={}", imageId);
    }

    private static ImageVerificationFailure classifyFailure(RuntimeException ex) {
        if (
                ex instanceof HttpClientErrorException.UnprocessableEntity rejected
                && rejected.getResponseBodyAsString().contains(NO_FACE_MESSAGE)
        ) {
            return ImageVerificationFailure.NO_FACE;
        }

        if (ex instanceof ImageStorageException) return ImageVerificationFailure.STORAGE_UNAVAILABLE;
        if (ex instanceof HttpClientErrorException) return ImageVerificationFailure.ANALYSIS_FAILED;
        if (ex instanceof RestClientException) return ImageVerificationFailure.ML_UNAVAILABLE;

        if (ex instanceof ResponseStatusException statusException) {
            return statusException.getStatusCode().is5xxServerError()
                    ? ImageVerificationFailure.ML_UNAVAILABLE
                    : ImageVerificationFailure.ANALYSIS_FAILED;
        }

        return ImageVerificationFailure.ANALYSIS_FAILED;
    }
}
