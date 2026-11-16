package com.identity.infrastructure.ml;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.identity.app.logging.LogExecutionTime;
import com.identity.infrastructure.ml.config.MlProperties;
import com.identity.infrastructure.ml.dto.PredictionResponseDTO;

@Component
@LogExecutionTime
@RequiredArgsConstructor
public class NeuralNetworkClient {
    private static final String FILE_PART_NAME = "file";

    private final RestTemplate restTemplate;
    private final MlProperties properties;

    public PredictionResponseDTO sendImageToPrediction(String filename, String contentType, byte[] content) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part(FILE_PART_NAME, content).filename(filename).contentType(MediaType.parseMediaType(contentType));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        PredictionResponseDTO prediction = restTemplate.postForObject(
                properties.predictUrl(),
                new HttpEntity<>(bodyBuilder.build(), httpHeaders),
                PredictionResponseDTO.class
        );

        if (prediction == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "ML service returned an empty response");
        }

        validatePrediction(prediction);
        return prediction;
    }

    private static void validatePrediction(PredictionResponseDTO prediction) {
        Double probability = prediction.fakeProbability();
        if (probability == null || !Double.isFinite(probability) || probability < 0.0 || probability > 1.0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "ML service returned an invalid probability");
        }

        if (prediction.predictedClass() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "ML service returned an invalid class");
        }

        PredictionClass predictedClass = PredictionClass
                .fromCode(prediction.predictedClass())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "ML service returned an invalid class"
                ));

        if (!predictedClass.getDescription().equals(prediction.classDescription())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "ML service returned inconsistent classification data"
            );
        }

        if (prediction.imageName() == null || prediction.imageName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "ML service returned an invalid image name");
        }
    }
}
