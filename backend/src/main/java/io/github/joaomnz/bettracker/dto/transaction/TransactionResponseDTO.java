package io.github.joaomnz.bettracker.dto.transaction;

import io.github.joaomnz.bettracker.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponseDTO(
        Long id,
        BigDecimal amount,
        TransactionType type,
        LocalDateTime createdAt
) {}
