package com.identity.app.api.dto;

import java.util.List;

public record SliceResponseDTO<T>(PaginationDTO pagination, List<T> items) { }
