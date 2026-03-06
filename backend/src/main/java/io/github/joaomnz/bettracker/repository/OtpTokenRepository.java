package io.github.joaomnz.bettracker.repository;

import io.github.joaomnz.bettracker.enums.OtpPurpose;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.model.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findTopByUserAndPurposeOrderByCreatedAtDesc(User user, OtpPurpose purpose);
}