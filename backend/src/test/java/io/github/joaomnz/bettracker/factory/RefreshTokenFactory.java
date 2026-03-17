package io.github.joaomnz.bettracker.factory;

import io.github.joaomnz.bettracker.model.RefreshToken;
import io.github.joaomnz.bettracker.model.User;

import java.time.LocalDateTime;

public class RefreshTokenFactory {
    public static final String VALID_RAW_TOKEN = "A".repeat(43);
    public static final String VALID_HASHED_TOKEN = "a0u7sYwKxL/bNnZzHqT+qXkX+r/1j0zZ2xY8xW+VqJw=";

    public static RefreshToken createValidToken(User user) {
        return new RefreshToken(
                user,
                VALID_HASHED_TOKEN,
                LocalDateTime.now().plusDays(7)
        );
    }

    public static RefreshToken createExpiredToken(User user) {
        return new RefreshToken(
                user,
                VALID_HASHED_TOKEN,
                LocalDateTime.now().minusDays(1)
        );
    }
}