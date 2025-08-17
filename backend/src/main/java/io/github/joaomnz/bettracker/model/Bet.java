package io.github.joaomnz.bettracker.model;

import io.github.joaomnz.bettracker.model.enums.BetStatus;
import io.github.joaomnz.bettracker.model.enums.StakeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bet")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
@Builder
public class Bet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String title;

    @Column(nullable = false)
    private String selection;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal stake;

    @Enumerated(EnumType.STRING)
    @Column(name = "stake_type", nullable = false)
    private StakeType stakeType = StakeType.VALUE;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal odds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BetStatus status = BetStatus.PENDING;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bettor_id", nullable = false)
    private Bettor bettor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookmaker_id")
    private Bookmaker bookmaker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipster_id")
    private Tipster tipster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id")
    private Sport sport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id")
    private Competition competition;

    public Bet() {
    }

    public Bet(Long id, String title, String selection, BigDecimal stake, StakeType stakeType, BigDecimal odds,
               BetStatus status, LocalDateTime eventDate, Bettor bettor, Bookmaker bookmaker, Tipster tipster, Sport sport,
               Competition competition) {
        this.id = id;
        this.title = title;
        this.selection = selection;
        this.stake = stake;
        this.stakeType = stakeType;
        this.odds = odds;
        this.status = status;
        this.eventDate = eventDate;
        this.bettor = bettor;
        this.bookmaker = bookmaker;
        this.tipster = tipster;
        this.sport = sport;
        this.competition = competition;
    }
}
