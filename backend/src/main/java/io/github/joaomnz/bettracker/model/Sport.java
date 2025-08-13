package io.github.joaomnz.bettracker.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sport")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
public class Sport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bettor_id", nullable = false)
    private Bettor bettor;

    @OneToMany(mappedBy = "sport", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Competition> competitions = new ArrayList<>();

    @OneToMany(mappedBy = "sport", fetch = FetchType.LAZY)
    private List<Bet> bets = new ArrayList<>();

    public Sport() {
    }

    public Sport(Long id, String name, Bettor bettor) {
        this.id = id;
        this.name = name;
        this.bettor = bettor;
    }
}
