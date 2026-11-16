package com.identity.core.admin.verification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.identity.persistence.user.Role;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VerificationUserDTO(
        Long id,
        Role role,
        String username,
        String email
) { }
