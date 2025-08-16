package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.dto.competition.CompetitionRequestDTO;
import io.github.joaomnz.bettracker.dto.competition.CompetitionResponseDTO;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Competition;
import io.github.joaomnz.bettracker.model.Sport;
import io.github.joaomnz.bettracker.security.BettorDetails;
import io.github.joaomnz.bettracker.service.CompetitionService;
import io.github.joaomnz.bettracker.service.SportService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RequestMapping("/api/v1/sports/{sportId}/competitions")
@RestController
public class CompetitionController {
    private final CompetitionService competitionService;
    private final SportService sportService;

    public CompetitionController(CompetitionService competitionService, SportService sportService) {
        this.competitionService = competitionService;
        this.sportService = sportService;
    }

    @PostMapping
    public ResponseEntity<CompetitionResponseDTO> create(@PathVariable Long sportId,
                                                         @Valid @RequestBody CompetitionRequestDTO request,
                                                         Authentication authentication){
        BettorDetails principal = (BettorDetails) authentication.getPrincipal();
        Bettor currentBettor = principal.getBettor();
        Sport parentSport = sportService.findByIdAndBettor(sportId, currentBettor);
        Competition createdCompetition = competitionService.create(request, parentSport);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCompetition.getId())
                .toUri();

        CompetitionResponseDTO responseDTO = new CompetitionResponseDTO(createdCompetition.getId(), createdCompetition.getName());
        return ResponseEntity.created(location).body(responseDTO);
    }
}
