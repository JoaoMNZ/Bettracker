package io.github.joaomnz.bettracker.repository;

import io.github.joaomnz.bettracker.model.Bettor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BettorRepository extends JpaRepository<Bettor, Long> {
    Optional<Bettor> findByEmail(String email);
}
