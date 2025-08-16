package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.dto.tipster.TipsterRequestDTO;
import io.github.joaomnz.bettracker.dto.tipster.TipsterResponseDTO;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Tipster;
import io.github.joaomnz.bettracker.security.BettorDetails;
import io.github.joaomnz.bettracker.service.TipsterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RequestMapping("/api/v1/tipsters")
@RestController
public class TipsterController {
    private final TipsterService tipsterService;

    public TipsterController(TipsterService tipsterService) {
        this.tipsterService = tipsterService;
    }

    @PostMapping
    public ResponseEntity<TipsterResponseDTO> create(@Valid @RequestBody TipsterRequestDTO request, Authentication authentication){
        BettorDetails principal = (BettorDetails) authentication.getPrincipal();
        Bettor currentBettor = principal.getBettor();
        Tipster createdTipster = tipsterService.create(request, currentBettor);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdTipster.getId()).toUri();
        TipsterResponseDTO responseDTO = new TipsterResponseDTO(createdTipster.getId(), createdTipster.getName());
        return ResponseEntity.created(location).body(responseDTO);
    }
}
