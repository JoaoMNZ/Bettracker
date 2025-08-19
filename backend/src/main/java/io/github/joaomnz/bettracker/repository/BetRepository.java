package io.github.joaomnz.bettracker.repository;

import io.github.joaomnz.bettracker.model.Bet;
import io.github.joaomnz.bettracker.model.Bettor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BetRepository extends JpaRepository<Bet, Long> {
    Optional<Bet> findByIdAndBettor(Long id, Bettor bettor);
    Page<Bet> findAllByBettor(Bettor bettor, Pageable pageable);
}
