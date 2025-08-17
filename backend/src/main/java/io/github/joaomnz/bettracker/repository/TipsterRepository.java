package io.github.joaomnz.bettracker.repository;

import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Tipster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipsterRepository extends JpaRepository<Tipster, Long> {
    Optional<Tipster> findByIdAndBettor(Long id, Bettor bettor);
}
