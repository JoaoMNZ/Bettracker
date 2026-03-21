package io.github.joaomnz.bettracker.dto.user;

import io.github.joaomnz.bettracker.enums.UserType;
import io.github.joaomnz.bettracker.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String name,
        String email,
        BigDecimal unitValue,
        UserType userType,
        boolean verified,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
){
    public static UserProfileResponse fromEntity(User user){
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getUnitValue(),
                user.getUserType(),
                user.isVerified(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
