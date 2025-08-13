package io.github.joaomnz.bettracker.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tipster")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
public class Tipster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bettor_id", nullable = false)
    private Bettor bettor;

    @OneToMany(mappedBy = "tipster", fetch = FetchType.LAZY)
    private List<Bet> bets = new ArrayList<>();

    public Tipster() {
    }

    public Tipster(Long id, String name, Bettor bettor) {
        this.id = id;
        this.name = name;
        this.bettor = bettor;
    }
}
