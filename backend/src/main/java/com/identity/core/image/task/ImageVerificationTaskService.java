package com.identity.core.image.task;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.identity.app.utils.UtcDateTimes;
import com.identity.core.image.service.ImageVerificationWriter;
import com.identity.core.image.task.dto.ImageVerificationTaskClaimDTO;
import com.identity.core.image.task.dto.ImageVerificationTaskStatusDTO;
import com.identity.core.image.task.lifecycle.ImageVerificationFailure;
import com.identity.core.image.task.lifecycle.ImageVerificationTaskStatus;
import com.identity.infrastructure.ml.dto.PredictionResponseDTO;
import com.identity.persistence.image.ImageRepository;
import com.identity.persistence.verification.task.VerificationTask;
import com.identity.persistence.verification.task.VerificationTaskRepository;

@Service
@RequiredArgsConstructor
public class ImageVerificationTaskService {
    private static final int MAX_ATTEMPTS = 3;
    private static final int DISPATCH_BATCH_SIZE = 50;
    private static final long DISPATCH_COOLDOWN_SECONDS = 45;
    private static final long PROCESSING_LEASE_SECONDS = 120;

    private final VerificationTaskRepository taskRepository;
    private final ImageRepository imageRepository;
    private final ImageVerificationWriter verificationWriter;

    @Transactional(readOnly = true)
    public ImageVerificationTaskStatusDTO getStatus(long userId, String imageId) {
        VerificationTask task = taskRepository.findByImageIdAndUserId(imageId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));

        return new ImageVerificationTaskStatusDTO(
                task.getImageId(),
                task.getStatus(),
                task.getPredictedClass(),
                task.getClassDescription(),
                task.getFakeProbability(),
                task.getErrorCode()
        );
    }

    @Transactional
    public ImageVerificationTaskClaimDTO claim(String imageId) {
        VerificationTask task = taskRepository.findForUpdate(imageId).orElse(null);
        LocalDateTime now = UtcDateTimes.nowMicros();

        if (task == null || task.getStatus() != ImageVerificationTaskStatus.PENDING || task.getNextAttemptAt().isAfter(now)) {
            return null;
        }

        task.setStatus(ImageVerificationTaskStatus.PROCESSING);
        task.setAttempts(task.getAttempts() + 1);
        task.setStartedAt(now);
        task.setErrorCode(null);

        String objectKey = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalStateException("Image missing for task " + imageId))
                .getFilepath();

        return new ImageVerificationTaskClaimDTO(
                task.getAttempts(),
                task.getUserId(),
                imageId,
                objectKey,
                task.getContentType()
        );
    }

    @Transactional
    public void complete(ImageVerificationTaskClaimDTO taskClaim, PredictionResponseDTO prediction) {
        VerificationTask task = taskRepository.findForUpdate(taskClaim.imageId())
                .orElseThrow(() -> new IllegalStateException("Verification task not found"));

        if (isStaleClaim(task, taskClaim)) return;

        LocalDateTime now = UtcDateTimes.nowMicros();
        verificationWriter.create(
                taskClaim.userId(),
                taskClaim.imageId(),
                now,
                prediction
        );

        task.setPredictedClass(prediction.predictedClass());
        task.setClassDescription(prediction.classDescription());
        task.setFakeProbability(prediction.fakeProbability());
        task.setStatus(ImageVerificationTaskStatus.COMPLETED);
        task.setFinishedAt(now);
        task.setErrorCode(null);
    }

    @Transactional
    public void fail(ImageVerificationTaskClaimDTO taskClaim, ImageVerificationFailure verificationFailure) {
        VerificationTask task = taskRepository.findForUpdate(taskClaim.imageId())
                .orElseThrow(() -> new IllegalStateException("Verification task not found"));

        if (isStaleClaim(task, taskClaim)) return;

        LocalDateTime now = UtcDateTimes.nowMicros();
        if (verificationFailure.isRetryable() && task.getAttempts() < MAX_ATTEMPTS) {
            task.setStatus(ImageVerificationTaskStatus.PENDING);
            task.setNextAttemptAt(now.plusSeconds(10L * task.getAttempts()));
            task.setStartedAt(null);
            task.setLastDispatchedAt(null);
        }
        else {
            task.setStatus(ImageVerificationTaskStatus.FAILED);
            task.setFinishedAt(now);
            task.setErrorCode(verificationFailure);
        }
    }

    @Transactional(readOnly = true)
    public List<String> findReadyToDispatch() {
        LocalDateTime now = UtcDateTimes.nowMicros();
        return taskRepository.findReadyToDispatch(
                ImageVerificationTaskStatus.PENDING,
                now,
                now.minusSeconds(DISPATCH_COOLDOWN_SECONDS),
                PageRequest.ofSize(DISPATCH_BATCH_SIZE)
        );
    }

    @Transactional
    public boolean reserveDispatch(String imageId) {
        LocalDateTime now = UtcDateTimes.nowMicros();
        return taskRepository.reserveDispatch(
                imageId,
                ImageVerificationTaskStatus.PENDING,
                now,
                now.minusSeconds(DISPATCH_COOLDOWN_SECONDS)
        ) == 1;
    }

    @Transactional
    public void recoverStaleTasks() {
        LocalDateTime now = UtcDateTimes.nowMicros();
        LocalDateTime cutoff = now.minusSeconds(PROCESSING_LEASE_SECONDS);

        taskRepository.failStaleTasks(
                MAX_ATTEMPTS,
                ImageVerificationTaskStatus.PROCESSING,
                ImageVerificationTaskStatus.FAILED,
                ImageVerificationFailure.PROCESSING_TIMEOUT,
                now,
                cutoff
        );
        taskRepository.recoverStaleTasks(
                MAX_ATTEMPTS,
                ImageVerificationTaskStatus.PROCESSING,
                ImageVerificationTaskStatus.PENDING,
                now,
                cutoff
        );
    }

    private static boolean isStaleClaim(VerificationTask task, ImageVerificationTaskClaimDTO taskClaim) {
        return task.getStatus() != ImageVerificationTaskStatus.PROCESSING || task.getAttempts() != taskClaim.attempt();
    }
}
