package com.identity.core.image.validation;

public record ImageUploadData(String contentType, String extension, byte[] content) { }
