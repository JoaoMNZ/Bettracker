package io.github.joaomnz.bettracker.dto.shared;

import java.util.List;

public record PageResponseDTO<T>(
        List<T> content,
        int currentPage,
        int pageSize,
        long totalElements,
        int totalPages
) {}