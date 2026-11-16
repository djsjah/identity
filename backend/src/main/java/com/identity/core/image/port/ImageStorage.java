package com.identity.core.image.port;

public interface ImageStorage {
    void save(String objectKey, String contentType, byte[] content);

    byte[] read(String objectKey);

    void delete(String objectKey);
}
