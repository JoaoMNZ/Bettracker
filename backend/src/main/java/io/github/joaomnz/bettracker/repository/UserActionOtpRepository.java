package io.github.joaomnz.bettracker.repository;

import io.github.joaomnz.bettracker.enums.ActionType;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.model.UserActionOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserActionOtpRepository extends JpaRepository<UserActionOtp, Long> {
    Optional<UserActionOtp> findTopByUserAndActionTypeOrderByCreatedAtDesc(User user, ActionType actionType);
}
