package io.github.joaomnz.bettracker.dto.auth;

import io.github.joaomnz.bettracker.dto.user.UserResponse;

public record AuthResponse(
        String refreshToken,
        String accessToken,
        UserResponse user
){}