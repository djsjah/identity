package com.identity.core.auth.dto;

import com.identity.persistence.user.Role;

public record AuthResponseDTO(String username, String email, Role role) { }
