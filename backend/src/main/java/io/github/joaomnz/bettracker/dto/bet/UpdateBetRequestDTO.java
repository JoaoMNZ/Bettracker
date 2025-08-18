package io.github.joaomnz.bettracker.dto.bet;

import io.github.joaomnz.bettracker.model.enums.BetStatus;
import io.github.joaomnz.bettracker.model.enums.StakeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateBetRequestDTO(
        @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters.")
        String title,

        @Size(min = 1, max = 255, message = "Selection must be between 1 and 255 characters.")
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
