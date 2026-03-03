package io.github.joaomnz.bettracker.dto;

import io.github.joaomnz.bettracker.enums.UserType;
import io.github.joaomnz.bettracker.model.User;

import java.math.BigDecimal;

public record UserResponse(
        Long id,
        String name,
        String email,
        BigDecimal unitValue,
        UserType userType
) {
    public static UserResponse fromEntity(User user){
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getUnitValue(),
                user.getUserType()
        );
    }
}
