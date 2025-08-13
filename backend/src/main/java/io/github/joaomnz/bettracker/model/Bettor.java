package io.github.joaomnz.bettracker.model;

import io.github.joaomnz.bettracker.model.enums.BettorType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bettor")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
public class Bettor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "unit_value", precision = 8, scale = 2)
    private BigDecimal unitValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BettorType type = BettorType.FREE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "bettor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Bookmaker> bookmakers = new ArrayList<>();

    @OneToMany(mappedBy = "bettor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "bettor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Tipster> tipsters = new ArrayList<>();

    @OneToMany(mappedBy = "bettor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Sport> sports = new ArrayList<>();

    @OneToMany(mappedBy = "bettor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Competition> competitions = new ArrayList<>();

    @OneToMany(mappedBy = "bettor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Bet> bets = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Bettor(){
    }

    public Bettor(Long id, String name, String email, String password, BigDecimal unitValue, BettorType type) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.unitValue = unitValue;
        this.type = type;
    }
}
