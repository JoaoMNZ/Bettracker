package io.github.joaomnz.bettracker.dto;

public record AuthResponse(
        String token,
        UserResponse userResponse
) {}
