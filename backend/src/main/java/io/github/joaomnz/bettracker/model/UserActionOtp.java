package io.github.joaomnz.bettracker.model;

import io.github.joaomnz.bettracker.enums.ActionType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_action_otps")
@NoArgsConstructor
@Getter @Setter
@EqualsAndHashCode(of = "id")
public class UserActionOtp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String otp;

    @Column(nullable = false, name = "action_type")
    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = false, name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @PrePersist
    void prePersist(){
        createdAt = LocalDateTime.now();
    }

    public UserActionOtp(User user, String otp, ActionType actionType, LocalDateTime expiresAt) {
        this.user = user;
        this.otp = otp;
        this.actionType = actionType;
        this.expiresAt = expiresAt;
    }
}
