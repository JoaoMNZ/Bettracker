package io.github.joaomnz.bettracker.model;

import io.github.joaomnz.bettracker.enums.ActionType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_action_tokens")
@NoArgsConstructor
@Getter @Setter
@EqualsAndHashCode(of = "id")
public class UserActionToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false, name = "action_type")
    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = false, name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    void prePersist(){
        createdAt = LocalDateTime.now();
    }
}
