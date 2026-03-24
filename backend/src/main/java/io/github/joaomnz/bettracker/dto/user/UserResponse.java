package io.github.joaomnz.bettracker.dto.user;

import io.github.joaomnz.bettracker.enums.UserType;
import io.github.joaomnz.bettracker.model.User;

import java.math.BigDecimal;

public record UserResponse(
        Long id,
        String name,
        String email,
        BigDecimal unitValue,
        UserType userType,
        boolean verified,
        boolean passwordEnabled,
        boolean googleLinked){
    public static UserResponse fromEntity(User user){
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getUnitValue(),
                user.getUserType(),
                user.isVerified(),
                user.getPassword() != null,
                user.getGoogleId() != null
        );
    }
}