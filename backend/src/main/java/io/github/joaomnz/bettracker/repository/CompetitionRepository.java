package io.github.joaomnz.bettracker.repository;

import io.github.joaomnz.bettracker.model.Competition;
import io.github.joaomnz.bettracker.model.Sport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {
    Optional<Competition> findByIdAndSport(Long id, Sport sport);
}
