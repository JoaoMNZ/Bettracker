package io.github.joaomnz.bettracker.dto.shared;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDTO(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> validationErrors,
        String path
) {}
