package io.github.joaomnz.bettracker.dto.auth;

import io.github.joaomnz.bettracker.model.enums.BettorType;

public record LoginResponseDTO(
        String token,
        Long id,
        String name,
        String email,
        BettorType type
) {}
