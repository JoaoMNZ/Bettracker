package io.github.joaomnz.bettracker.dto.sport;

import jakarta.validation.constraints.NotBlank;

public record SportRequestDTO(
        @NotBlank(message = "The name is required.")
        String name
) {}
