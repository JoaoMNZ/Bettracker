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
    private String code;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OtpPurpose purpose;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = false, name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(nullable = false, name = "failed_attempts")
    private int failedAttempts = 0;

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
