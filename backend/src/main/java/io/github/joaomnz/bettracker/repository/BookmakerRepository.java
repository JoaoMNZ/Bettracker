package io.github.joaomnz.bettracker.repository;

import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Bookmaker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookmakerRepository extends JpaRepository<Bookmaker, Long> {
    Optional<Bookmaker> findByIdAndBettor(Long bookmakerId, Bettor bettor);
}
