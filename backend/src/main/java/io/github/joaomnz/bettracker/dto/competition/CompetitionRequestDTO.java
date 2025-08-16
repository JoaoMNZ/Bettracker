package io.github.joaomnz.bettracker.dto.competition;

import jakarta.validation.constraints.NotBlank;

public record CompetitionRequestDTO(
        @NotBlank(message = "The name is required.")
        String name
) {}
