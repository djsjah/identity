package com.identity.infrastructure.ml.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PredictionResponseDTO(
        @JsonProperty(value = "class", required = true)
        Integer predictedClass,

        @JsonProperty(value = "fake_probability", required = true)
        Double fakeProbability,

        @JsonProperty(value = "class_description", required = true)
        String classDescription,

        @JsonProperty(value = "image_name", required = true)
        String imageName
) { }
