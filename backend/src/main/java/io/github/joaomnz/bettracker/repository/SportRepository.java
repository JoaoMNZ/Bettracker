package io.github.joaomnz.bettracker.repository;

import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Sport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SportRepository extends JpaRepository<Sport, Long> {
    Optional<Sport> findByIdAndBettor(Long id, Bettor bettor);
    Page<Sport> findAllByBettor(Bettor bettor, Pageable pageable);
}
