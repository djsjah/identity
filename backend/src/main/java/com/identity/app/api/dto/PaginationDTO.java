package com.identity.app.api.dto;

public record PaginationDTO(
        int page,
        int size,
        int elements,
        boolean first,
        boolean last,
        boolean hasNext,
        boolean hasPrevious
) { }
