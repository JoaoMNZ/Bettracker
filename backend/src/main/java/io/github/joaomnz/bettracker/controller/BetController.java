package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.dto.bet.BetResponseDTO;
import io.github.joaomnz.bettracker.dto.bet.CreateBetRequestDTO;
import io.github.joaomnz.bettracker.dto.bet.UpdateBetRequestDTO;
import io.github.joaomnz.bettracker.mappers.BetMapper;
import io.github.joaomnz.bettracker.model.*;
import io.github.joaomnz.bettracker.security.BettorDetails;
import io.github.joaomnz.bettracker.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
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

    private final BetMapper betMapper;

    public BetController(BetService betService,
                         BookmakerService bookmakerService,
                         TipsterService tipsterService,
                         SportService sportService,
                         CompetitionService competitionService,
                         BetMapper betMapper) {
        this.betService = betService;
        this.bookmakerService = bookmakerService;
        this.tipsterService = tipsterService;
        this.sportService = sportService;
        this.competitionService = competitionService;
        this.betMapper = betMapper;
    }

    @PostMapping
    public ResponseEntity<BetResponseDTO> create(@Valid @RequestBody CreateBetRequestDTO request, Authentication authentication){
        BettorDetails principal = (BettorDetails) authentication.getPrincipal();
        Bettor currentBettor = principal.getBettor();

        Bookmaker associatedBookmaker = null;
        Tipster associatedTipster = null;
        Sport associatedSport = null;
        Competition associatedCompetition = null;

        if(request.bookmakerId() != null) associatedBookmaker = bookmakerService.findByIdAndBettor(request.bookmakerId(), currentBettor);
        if(request.tipsterId() != null) associatedTipster = tipsterService.findByIdAndBettor(request.tipsterId(), currentBettor);
        if(request.sportId() != null) associatedSport = sportService.findByIdAndBettor(request.sportId(), currentBettor);
        if(request.competitionId() != null && associatedSport != null) associatedCompetition = competitionService.findByIdAndSport(request.competitionId(), associatedSport);
        Bet createdBet = betService.create(request, associatedBookmaker, associatedTipster, associatedSport, associatedCompetition, currentBettor);
        BetResponseDTO responseDTO = betMapper.toDto(createdBet);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdBet.getId())
                .toUri();

        return ResponseEntity.created(location).body(responseDTO);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BetResponseDTO> update(@PathVariable Long id, @Valid @RequestBody UpdateBetRequestDTO request, Authentication authentication) {
        BettorDetails principal = (BettorDetails) authentication.getPrincipal();
        Bettor currentBettor = principal.getBettor();

        Bookmaker associatedBookmaker = null;
        Tipster associatedTipster = null;
        Sport associatedSport = null;
        Competition associatedCompetition = null;

        if (request.bookmakerId() != null) associatedBookmaker = bookmakerService.findByIdAndBettor(request.bookmakerId(), currentBettor);
        if (request.tipsterId() != null) associatedTipster = tipsterService.findByIdAndBettor(request.tipsterId(), currentBettor);
        if (request.sportId() != null) associatedSport = sportService.findByIdAndBettor(request.sportId(), currentBettor);
        if (request.competitionId() != null && associatedSport != null) associatedCompetition = competitionService.findByIdAndSport(request.competitionId(), associatedSport);

        Bet updatedBet = betService.update(id, request, associatedBookmaker, associatedTipster, associatedSport, associatedCompetition, currentBettor);

        BetResponseDTO responseDTO = betMapper.toDto(updatedBet);

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication){
        BettorDetails principal = (BettorDetails) authentication.getPrincipal();
        Bettor currentBettor = principal.getBettor();

        betService.delete(id, currentBettor);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
