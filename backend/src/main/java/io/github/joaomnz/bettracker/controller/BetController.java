package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.dto.bet.CreateBetRequestDTO;
import io.github.joaomnz.bettracker.dto.bet.CreateBetResponseDTO;
import io.github.joaomnz.bettracker.model.*;
import io.github.joaomnz.bettracker.security.BettorDetails;
import io.github.joaomnz.bettracker.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RequestMapping("/api/v1/bets")
@RestController
public class BetController {
    private final BetService betService;
    private final BookmakerService bookmakerService;
    private final TipsterService tipsterService;
    private final SportService sportService;
    private final CompetitionService competitionService;

    public BetController(BetService betService,
                         BookmakerService bookmakerService,
                         TipsterService tipsterService,
                         SportService sportService,
                         CompetitionService competitionService) {
        this.betService = betService;
        this.bookmakerService = bookmakerService;
        this.tipsterService = tipsterService;
        this.sportService = sportService;
        this.competitionService = competitionService;
    }

    @PostMapping
    public ResponseEntity<CreateBetResponseDTO> create(@Valid @RequestBody CreateBetRequestDTO request, Authentication authentication){
        BettorDetails principal = (BettorDetails) authentication.getPrincipal();
        Bettor currentBettor = principal.getBettor();

        Bookmaker associatedBookmaker = null;
        Tipster associatedTipster = null;
        Sport associatedSport = null;
        Competition associatedCompetition = null;

        if(request.BookmakerId() != null) associatedBookmaker = bookmakerService.findByIdAndBettor(request.BookmakerId(), currentBettor);
        if(request.tipsterId() != null) associatedTipster = tipsterService.findByIdAndBettor(request.tipsterId(), currentBettor);
        if(request.sportId() != null) associatedSport = sportService.findByIdAndBettor(request.sportId(), currentBettor);
        if(request.competitionId() != null && associatedSport != null) associatedCompetition = competitionService.findByIdAndSport(request.competitionId(), associatedSport);

        Bet createdBet = betService.create(request, associatedBookmaker, associatedTipster, associatedSport, associatedCompetition, currentBettor);

        CreateBetResponseDTO responseDTO = new CreateBetResponseDTO(
                createdBet.getId(),
                createdBet.getTitle(),
                createdBet.getSelection(),
                createdBet.getStake(),
                createdBet.getOdds(),
                createdBet.getStatus(),
                createdBet.getEventDate(),
                associatedBookmaker != null ? associatedBookmaker.getName() : null,
                associatedTipster != null ? associatedTipster.getName() : null,
                associatedSport != null ? associatedSport.getName() : null,
                associatedCompetition != null ? associatedCompetition.getName() : null
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdBet.getId())
                .toUri();

        return ResponseEntity.created(location).body(responseDTO);
    }
}
