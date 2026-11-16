package com.identity.core.admin.verification.dto;

import java.time.LocalDateTime;

import com.identity.core.admin.image.dto.ImageDTO;
import com.identity.persistence.user.Role;

public record VerificationHistoryDetailsDTO(
        String id,
        ImageDTO image,
        VerificationUserDTO user,
        UserSnapshot userAtVerification,
        LocalDateTime verificationDate,
        String result
) {
    public record UserSnapshot(Role role, String email) { }
}
