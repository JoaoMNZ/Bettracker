package io.github.joaomnz.bettracker.model;

import io.github.joaomnz.bettracker.enums.OtpPurpose;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_tokens")
@NoArgsConstructor
@Getter @Setter
@EqualsAndHashCode(of = "id")
public class OtpToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OtpPurpose purpose;

    @Column(nullable = false)
    private String code;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @PrePersist
    void prePersist(){
        createdAt = LocalDateTime.now();
    }

    public OtpToken(User user, String code, OtpPurpose purpose, LocalDateTime expiresAt) {
        this.user = user;
        this.code = code;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }
}