package io.github.joaomnz.bettracker.dto.auth;

public record LoginRequestDTO(
    String email,
    String password
) {}
