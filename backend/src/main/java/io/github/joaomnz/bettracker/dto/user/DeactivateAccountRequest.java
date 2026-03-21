package io.github.joaomnz.bettracker.dto.user;

import jakarta.validation.constraints.NotBlank;

public record DeactivateAccountRequest(
        @NotBlank(message = "Password is required to deactivate your account.")
        String password
){}
