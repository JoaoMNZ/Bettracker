package io.github.joaomnz.bettracker.repository;

import io.github.joaomnz.bettracker.model.UserActionToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActionTokenRepository extends JpaRepository<UserActionToken, Long> {
}
