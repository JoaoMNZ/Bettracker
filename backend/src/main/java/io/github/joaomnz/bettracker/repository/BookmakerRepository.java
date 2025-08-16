package io.github.joaomnz.bettracker.repository;

import io.github.joaomnz.bettracker.model.Bookmaker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmakerRepository extends JpaRepository<Bookmaker, Long> {
}
