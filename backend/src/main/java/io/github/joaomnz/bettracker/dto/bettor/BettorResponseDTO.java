package io.github.joaomnz.bettracker.dto.bettor;

import io.github.joaomnz.bettracker.model.enums.BettorType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BettorResponseDTO(
        Long id,
        String name,
        String email,
        BettorType type,
        BigDecimal unitValue,
        LocalDateTime createdAt
) {}
