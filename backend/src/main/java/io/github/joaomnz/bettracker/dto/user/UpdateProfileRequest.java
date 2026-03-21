package io.github.joaomnz.bettracker.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateProfileRequest(
        @Size(min = 1, max = 255, message = "The name must be between 1 and 255 characters.")
        @Pattern(regexp = ".*\\S.*", message = "The name must not be blank.")
        String name,

        @Positive(message = "The unit value must be positive.")
        @Digits(integer = 15, fraction = 4, message = "The unit value must have a maximum of 15 integer digits and 4 fractional digits.")
        BigDecimal unitValue
){
        @JsonIgnore
        @AssertTrue(message = "You must provide at least a name or a unit value to update.")
        public boolean isValidUpdate() {
                return name != null || unitValue != null;
        }
}
