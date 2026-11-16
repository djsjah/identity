package com.identity.infrastructure.ml;

import java.util.Optional;

import lombok.Getter;

@Getter
public enum PredictionClass {
    REAL(0, "Real"),
    FAKE(1, "Fake");

    private final int code;
    private final String description;

    PredictionClass(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static Optional<PredictionClass> fromCode(int code) {
        for (PredictionClass value : values()) if (value.code == code) return Optional.of(value);
        return Optional.empty();
    }
}
