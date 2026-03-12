package io.github.joaomnz.bettracker.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(
        @NotBlank(message = "Current password is required.")
        String oldPassword,

        @NotBlank(message = "The password is required.")
        @Size(max = 255, message = "The password must not be more than 255 characters.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "The password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character."
        )
        String newPassword
) {}
