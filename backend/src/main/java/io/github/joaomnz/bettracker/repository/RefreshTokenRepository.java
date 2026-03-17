package io.github.joaomnz.bettracker.repository;

import io.github.joaomnz.bettracker.model.RefreshToken;
import io.github.joaomnz.bettracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findAllByUserOrderByExpiresAtAsc(User user);
}