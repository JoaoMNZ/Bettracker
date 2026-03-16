package io.github.joaomnz.bettracker.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailVerificationRequest(
        @NotBlank(message = "OTP is required.")
        @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits.")
        String otp
) {}
