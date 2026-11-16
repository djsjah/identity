package com.identity.core.image.port;

public interface ImageVerificationTaskPublisher {
    void publish(String imageId);
}
