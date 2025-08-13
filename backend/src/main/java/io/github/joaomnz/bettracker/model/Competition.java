package io.github.joaomnz.bettracker.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "competition")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
public class Competition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bettor_id", nullable = false)
    private Bettor bettor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id", nullable = false)
    private Sport sport;

    @OneToMany(mappedBy = "competition", fetch = FetchType.LAZY)
    private List<Bet> bets = new ArrayList<>();

    public Competition() {
    }

    public Competition(Long id, String name, Bettor bettor, Sport sport) {
        this.id = id;
        this.name = name;
        this.bettor = bettor;
        this.sport = sport;
    }
}
