package com.identity.infrastructure.s3;

import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import com.identity.core.image.exception.ImageStorageException;
import com.identity.core.image.port.ImageStorage;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3ImageStorage implements ImageStorage {
    private final S3Client s3Client;
    private final S3StorageProperties properties;

    @Override
    public void save(String objectKey, String contentType, byte[] content) {
        validateSize(content.length);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(contentType)
                .build();

        execute(() -> s3Client.putObject(request, RequestBody.fromBytes(content)));
        log.info("Image stored: key={}", objectKey);
    }

    @Override
    public byte[] read(String objectKey) {
        return execute(() -> {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    .build();

            validateSize(s3Client.headObject(headRequest).contentLength());

            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    .build();

            byte[] content = s3Client.getObjectAsBytes(getRequest).asByteArray();
            validateSize(content.length);
            return content;
        });
    }

    @Override
    public void delete(String objectKey) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();

        execute(() -> s3Client.deleteObject(deleteRequest));
        log.info("Image deleted: key={}", objectKey);
    }

    private void validateSize(long size) {
        if (size <= 0 || size > properties.maxObjectSize().toBytes()) {
            throw new ImageStorageException("Image object has an invalid size");
        }
    }

    private <T> T execute(Supplier<T> action) {
        try {
            return action.get();
        }
        catch (SdkException ex) {
            throw new ImageStorageException("Image storage operation failed", ex);
        }
    }
}
