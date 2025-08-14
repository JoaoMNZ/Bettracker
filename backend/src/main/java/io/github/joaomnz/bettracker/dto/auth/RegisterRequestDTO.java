package io.github.joaomnz.bettracker.dto.auth;

public record RegisterRequestDTO(
        String name,
        String email,
        String password
) {}
