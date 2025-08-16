package io.github.joaomnz.bettracker.repository;

import io.github.joaomnz.bettracker.model.Sport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SportRepository extends JpaRepository<Sport, Long> {
}
