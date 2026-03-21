package io.github.joaomnz.bettracker.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "The email is required.")
        @Email(message = "Invalid email format.")
        String email,

        @NotBlank(message = "OTP is required")
        @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
        String otp,

        @NotBlank(message = "The new password is required.")
        @Size(max = 255, message = "The password must not be more than 255 characters.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "The password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character."
        )
        String newPassword
){}
