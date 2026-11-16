package com.identity.core.admin.verification.dto;

public record VerificationHistorySearchDTO(
        String imageId,
        Integer userId,
        String verificationDate,
        String result
) { }
