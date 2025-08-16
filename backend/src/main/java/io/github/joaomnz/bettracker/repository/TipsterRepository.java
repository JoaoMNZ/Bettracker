package io.github.joaomnz.bettracker.repository;

import io.github.joaomnz.bettracker.model.Tipster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipsterRepository extends JpaRepository<Tipster, Long> {
}
