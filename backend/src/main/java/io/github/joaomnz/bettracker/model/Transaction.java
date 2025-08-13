package io.github.joaomnz.bettracker.model;

import io.github.joaomnz.bettracker.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bettor_id", nullable = false)
    private Bettor bettor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookmaker_id", nullable = false)
    private Bookmaker bookmaker;

    @PrePersist
    public void prePersist(){
        createdAt = LocalDateTime.now();
    }

    public Transaction() {
    }

    public Transaction(Long id, BigDecimal amount, TransactionType type, Bettor bettor, Bookmaker bookmaker) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.bettor = bettor;
        this.bookmaker = bookmaker;
    }
}
