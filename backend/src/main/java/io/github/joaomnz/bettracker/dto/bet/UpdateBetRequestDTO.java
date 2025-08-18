package io.github.joaomnz.bettracker.dto.bet;

import io.github.joaomnz.bettracker.model.enums.BetStatus;
import io.github.joaomnz.bettracker.model.enums.StakeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateBetRequestDTO(
        @NotBlank(message = "Title cannot be blank if provided.")
        String title,

        @NotBlank(message = "Selection cannot be blank if provided.")
        String selection,

        @Positive(message = "Stake must be positive.")
        BigDecimal stake,

        StakeType stakeType,

        @DecimalMin(value = "1.01", message = "Odds must be greater than 1.00.")
        BigDecimal odds,

        BetStatus status,
        LocalDateTime eventDate,
        Long bookmakerId,
        Long tipsterId,
        Long sportId,
        Long competitionId
) {}
