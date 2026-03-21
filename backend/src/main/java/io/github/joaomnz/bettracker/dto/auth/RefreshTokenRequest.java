package io.github.joaomnz.bettracker.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required.")
        @Size(min = 43, max = 43, message = "Invalid token format.")
        String refreshToken
){}