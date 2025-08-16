package io.github.joaomnz.bettracker.dto.transaction;

import io.github.joaomnz.bettracker.model.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransactionRequestDTO(
        @NotNull(message = "Amount cannot be null.")
        @Positive(message = "Amount must be positive.")
        BigDecimal amount,

        @NotNull(message = "Transaction type cannot be null.")
        TransactionType type
) {}
