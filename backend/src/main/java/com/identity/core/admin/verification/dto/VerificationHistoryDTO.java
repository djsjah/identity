package com.identity.core.admin.verification.dto;

import java.time.LocalDateTime;

import com.identity.core.admin.image.dto.ImageDTO;

public record VerificationHistoryDTO(
        String id,
        ImageDTO image,
        VerificationUserDTO user,
        LocalDateTime verificationDate,
        String result
) { }
