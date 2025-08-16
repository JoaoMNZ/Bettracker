package io.github.joaomnz.bettracker.dto.bookmaker;

import jakarta.validation.constraints.NotBlank;

public record BookmakerRequestDTO(
        @NotBlank
        String name
) {}
