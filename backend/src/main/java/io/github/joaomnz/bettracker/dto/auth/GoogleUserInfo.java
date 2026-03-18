package io.github.joaomnz.bettracker.dto.auth;

public record GoogleUserInfo(
        String email,
        String name,
        String googleId,
        boolean emailVerified
){}
