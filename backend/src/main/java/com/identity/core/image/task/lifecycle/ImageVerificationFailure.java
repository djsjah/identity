package com.identity.core.image.task.lifecycle;

import lombok.Getter;

@Getter
public enum ImageVerificationFailure {
    NO_FACE(false),
    ML_UNAVAILABLE(true),
    STORAGE_UNAVAILABLE(true),
    ANALYSIS_FAILED(false),
    PROCESSING_TIMEOUT(true);

    private final boolean retryable;

    ImageVerificationFailure(boolean retryable) {
        this.retryable = retryable;
    }
}
