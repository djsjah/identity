package com.identity.core.user.version.dto;

import java.time.LocalDateTime;

import com.identity.persistence.user.Role;

public record UserVersionDTO(
        Long id,
        Integer version,
        Role role,
        String username,
        String email,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        String changeReason
) { }
