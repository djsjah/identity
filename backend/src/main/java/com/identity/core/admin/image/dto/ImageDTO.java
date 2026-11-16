package com.identity.core.admin.image.dto;

import java.time.LocalDateTime;

public record ImageDTO(String id, String filepath, LocalDateTime uploadDate) { }
