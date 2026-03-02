    package io.github.joaomnz.bettracker.model;

    import io.github.joaomnz.bettracker.enums.UserType;
    import jakarta.persistence.*;
    import lombok.*;

    import java.math.BigDecimal;
    import java.time.LocalDateTime;

    @Entity
    @Table(name = "users")
    @NoArgsConstructor
    @Getter @Setter
    @EqualsAndHashCode(of = "id")
    public class User {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false)
        private String name;

        @Column(nullable = false, unique = true)
        private String email;

        @Column(nullable = false)
        private String password;

        @Column(nullable = false, name = "unit_value", precision = 19, scale = 4)
        private BigDecimal unitValue;

        @Column(nullable = false, name = "user_type")
        @Enumerated(EnumType.STRING)
        private UserType userType;

        @Column(nullable = false)
        private boolean active;

        @Column(nullable = false, updatable = false, name = "created_at")
        private LocalDateTime createdAt;

        @Column(name = "updated_at")
        private LocalDateTime updatedAt;

        @PrePersist
        public void prePersist(){
            createdAt = LocalDateTime.now();
            userType = UserType.FREE;
            active = true;
        }

        @PreUpdate
        public void preUpdate(){
            updatedAt = LocalDateTime.now();
        }
    }