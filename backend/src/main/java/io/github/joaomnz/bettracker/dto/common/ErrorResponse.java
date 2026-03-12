package io.github.joaomnz.bettracker.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,

        // This hides the field from the JSON if it's null or empty
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, List<String>> validationErrors,

        String path
) {}