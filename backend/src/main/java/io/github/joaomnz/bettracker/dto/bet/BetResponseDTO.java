package io.github.joaomnz.bettracker.dto.bet;

import io.github.joaomnz.bettracker.model.enums.BetStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BetResponseDTO(
        Long id,
        String title,
        String selection,
        BigDecimal stake,
        BigDecimal odds,
        BetStatus status,
        LocalDateTime eventDate,
        String bookmaker,
        String tipster,
        String sport,
        String competition
) {}
