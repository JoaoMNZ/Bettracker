package io.github.joaomnz.bettracker.dto.sport;

import jakarta.validation.constraints.NotBlank;

public record SportRequestDTO(
        @NotBlank
        String name
) {}
