package io.github.joaomnz.bettracker.repository;

import io.github.joaomnz.bettracker.model.Bet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BetRepository extends JpaRepository<Bet, Long> {
}
