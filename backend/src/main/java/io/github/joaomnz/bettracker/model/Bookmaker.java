package io.github.joaomnz.bettracker.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookmaker")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
public class Bookmaker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bettor_id", nullable = false)
    private Bettor bettor;

    @OneToMany(mappedBy = "bookmaker", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "bookmaker", fetch = FetchType.LAZY)
    private List<Bet> bets = new ArrayList<>();

    public Bookmaker() {

    }

    public Bookmaker(Long id, String name, Bettor bettor) {
        this.id = id;
        this.name = name;
        this.bettor = bettor;
    }
}
