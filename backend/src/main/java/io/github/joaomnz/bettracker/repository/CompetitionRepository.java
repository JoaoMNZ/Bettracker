package io.github.joaomnz.bettracker.repository;

import io.github.joaomnz.bettracker.model.Competition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {
}
