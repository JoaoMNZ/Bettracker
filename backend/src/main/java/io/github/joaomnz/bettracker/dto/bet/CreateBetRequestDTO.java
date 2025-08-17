package io.github.joaomnz.bettracker.dto.bet;

import io.github.joaomnz.bettracker.model.enums.BetStatus;
import io.github.joaomnz.bettracker.model.enums.StakeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateBetRequestDTO(
        String title,

        @NotBlank(message = "The selection is required.")
        String selection,

        @Positive(message = "Stake must be positive.")
        @NotNull(message = "Stake cannot be null.")
        BigDecimal stake,

        StakeType stakeType,

        @Positive(message = "Odds must be positive.")
        @NotNull(message = "Odds cannot be null.")
        BigDecimal odds,

        BetStatus status,

        LocalDateTime eventDate,

        Long BookmakerId,
        Long tipsterId,
        Long sportId,
        Long competitionId
) {}