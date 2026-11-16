package com.identity.persistence.verification.task;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.*;
import lombok.*;

import com.identity.core.image.task.lifecycle.ImageVerificationFailure;
import com.identity.core.image.task.lifecycle.ImageVerificationTaskStatus;

@Entity
@Table(name = "verification_task")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationTask {
    @Id
    @Column(name = "image_id", nullable = false)
    private String imageId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "content_type", nullable = false, length = 32)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImageVerificationTaskStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_dispatched_at")
    private LocalDateTime lastDispatchedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_code", length = 32)
    private ImageVerificationFailure errorCode;

    @Column(name = "predicted_class")
    private Integer predictedClass;

    @Column(name = "class_description", length = 16)
    private String classDescription;

    @Column(name = "fake_probability")
    private Double fakeProbability;

    @Builder
    private VerificationTask(
            int attempts,
            String imageId,
            Long userId,
            String contentType,
            ImageVerificationTaskStatus status,
            LocalDateTime createdAt,
            LocalDateTime nextAttemptAt
    ) {
        this.attempts = attempts;
        this.imageId = Objects.requireNonNull(imageId);
        this.userId = Objects.requireNonNull(userId);
        this.contentType = Objects.requireNonNull(contentType);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.nextAttemptAt = Objects.requireNonNull(nextAttemptAt);
    }
}
