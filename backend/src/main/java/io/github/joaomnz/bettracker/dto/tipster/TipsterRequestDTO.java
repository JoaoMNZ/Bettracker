package io.github.joaomnz.bettracker.dto.tipster;

import jakarta.validation.constraints.NotBlank;

public record TipsterRequestDTO(
        @NotBlank(message = "The name is required.")
        String name
) {}
