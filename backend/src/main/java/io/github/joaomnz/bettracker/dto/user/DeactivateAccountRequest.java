package io.github.joaomnz.bettracker.dto.user;

import jakarta.validation.constraints.Size;

public record DeactivateAccountRequest(
        @Size(max = 255, message = "The password must not be more than 255 characters.")
        String password
){}