package io.github.joaomnz.bettracker.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestEmailChangeRequest(
        @NotBlank(message = "The email is required.")
        @Size(max = 255, message = "The email must not be more than 255 characters.")
        @Email(message = "Invalid email format.")
        String newEmail,

        @NotBlank(message = "Current password is required.")
        String currentPassword
) {}
