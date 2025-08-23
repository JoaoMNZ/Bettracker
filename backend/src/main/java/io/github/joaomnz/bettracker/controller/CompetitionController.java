package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.dto.competition.CompetitionRequestDTO;
import io.github.joaomnz.bettracker.dto.competition.CompetitionResponseDTO;
import io.github.joaomnz.bettracker.dto.shared.PageResponseDTO;
import io.github.joaomnz.bettracker.mapper.CompetitionMapper;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Competition;
import io.github.joaomnz.bettracker.model.Sport;
import io.github.joaomnz.bettracker.security.BettorDetails;
import io.github.joaomnz.bettracker.service.CompetitionService;
import io.github.joaomnz.bettracker.service.SportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RequestMapping("/api/v1/sports/{sportId}/competitions")
@RestController
public class CompetitionController {
    private final CompetitionService competitionService;
    private final SportService sportService;
    private final CompetitionMapper competitionMapper;

    public CompetitionController(CompetitionService competitionService, SportService sportService, CompetitionMapper competitionMapper) {
        this.competitionService = competitionService;
        this.sportService = sportService;
        this.competitionMapper = competitionMapper;
    }

    @GetMapping("/{competitionId}")
    public ResponseEntity<CompetitionResponseDTO> findById(@PathVariable Long sportId,
                                                           @PathVariable Long competitionId,
                                                           Authentication authentication){
        Bettor currentBettor = getBettor(authentication);

        Sport parentSport = sportService.findByIdAndBettor(sportId, currentBettor);
        Competition foundCompetition = competitionService.findByIdAndSport(competitionId, parentSport);

        CompetitionResponseDTO responseDTO = competitionMapper.toDto(foundCompetition);

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<CompetitionResponseDTO>> findAll(@PathVariable Long sportId,
                                                                           Authentication authentication,
                                                                           Pageable pageable){
        Bettor currentBettor = getBettor(authentication);

        Sport parentSport = sportService.findByIdAndBettor(sportId, currentBettor);
        Page<Competition> competitionPage = competitionService.findAllBySport(parentSport, pageable);

        List<CompetitionResponseDTO> competitionsDTO = competitionPage.getContent().stream()
                .map(competitionMapper::toDto)
                .toList();

        PageResponseDTO<CompetitionResponseDTO> responseDTO = new PageResponseDTO<>(
                competitionsDTO,
                competitionPage.getNumber(),
                competitionPage.getSize(),
                competitionPage.getTotalElements(),
                competitionPage.getTotalPages()
        );

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    @PostMapping
    public ResponseEntity<CompetitionResponseDTO> create(@PathVariable Long sportId,
                                                         @Valid @RequestBody CompetitionRequestDTO request,
                                                         Authentication authentication){
        Bettor currentBettor = getBettor(authentication);
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

    @PutMapping("/{competitionId}")
    public ResponseEntity<CompetitionResponseDTO> update(@PathVariable Long sportId,
                                                         @PathVariable Long competitionId,
                                                         @Valid @RequestBody CompetitionRequestDTO request,
                                                         Authentication authentication){
        Bettor currentBettor = getBettor(authentication);

        Sport parentSport = sportService.findByIdAndBettor(sportId, currentBettor);
        Competition updatedCompetition = competitionService.update(competitionId, request, parentSport);

        CompetitionResponseDTO responseDTO = competitionMapper.toDto(updatedCompetition);

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    @DeleteMapping("/{competitionId}")
    public ResponseEntity<Void> delete(@PathVariable Long sportId,
                                       @PathVariable Long competitionId,
                                       Authentication authentication){
        Bettor currentBettor = getBettor(authentication);

        Sport parentSport = sportService.findByIdAndBettor(sportId, currentBettor);
        competitionService.delete(competitionId, parentSport);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    public Bettor getBettor(Authentication authentication){
        BettorDetails principal = (BettorDetails) authentication.getPrincipal();
        return principal.getBettor();
    }
}
