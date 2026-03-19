package io.github.joaomnz.bettracker.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "Google token is required.")
        String token
) {}