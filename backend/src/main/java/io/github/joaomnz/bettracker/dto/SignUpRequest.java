package io.github.joaomnz.bettracker.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SignUpRequest(
        @NotBlank(message = "The name is required.")
        @Size(max = 255, message = "The name must not be more than 255 characters.")
        String name,

        @NotBlank(message = "The email is required.")
        @Size(max = 255, message = "The email must not be more than 255 characters.")
        @Email(message = "Invalid email format.")
        String email,

        @NotBlank(message = "The password is required.")
        @Size(max = 255, message = "The password must not be more than 255 characters.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "The password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character."
        )
        String password,

        @NotNull(message = "The unit value is required.")
        @Positive(message = "The unit value must be positive.")
        @Digits(integer = 15, fraction = 4, message = "The unit value must have a maximum of 15 integer digits and 4 fractional digits.")
        BigDecimal unitValue
) {}
