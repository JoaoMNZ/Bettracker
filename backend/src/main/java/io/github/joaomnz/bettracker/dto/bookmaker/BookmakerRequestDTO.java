package io.github.joaomnz.bettracker.dto.bookmaker;

import jakarta.validation.constraints.NotBlank;

public record BookmakerRequestDTO(
        @NotBlank(message = "The name is required.")
        String name
) {}
