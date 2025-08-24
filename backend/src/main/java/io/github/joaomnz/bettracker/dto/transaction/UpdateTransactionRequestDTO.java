package io.github.joaomnz.bettracker.dto.transaction;

import io.github.joaomnz.bettracker.model.enums.TransactionType;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateTransactionRequestDTO(
        @Positive(message = "Amount must be positive.")
        BigDecimal amount,

        TransactionType type
) {}
