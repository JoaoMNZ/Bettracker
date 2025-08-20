package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.dto.shared.PageResponseDTO;
import io.github.joaomnz.bettracker.dto.tipster.TipsterRequestDTO;
import io.github.joaomnz.bettracker.dto.tipster.TipsterResponseDTO;
import io.github.joaomnz.bettracker.mapper.TipsterMapper;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Tipster;
import io.github.joaomnz.bettracker.security.BettorDetails;
import io.github.joaomnz.bettracker.service.TipsterService;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


import java.net.URI;
import java.util.List;

@RequestMapping("/api/v1/tipsters")
@RestController
public class TipsterController {
    private final TipsterService tipsterService;
    private final TipsterMapper tipsterMapper;

    public TipsterController(TipsterService tipsterService, TipsterMapper tipsterMapper) {
        this.tipsterService = tipsterService;
        this.tipsterMapper = tipsterMapper;
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipsterResponseDTO> findById(@PathVariable Long id, Authentication authentication){
        Bettor currentBettor = getBettor(authentication);

        Tipster foundTipster = tipsterService.findByIdAndBettor(id, currentBettor);

        TipsterResponseDTO responseDTO = new TipsterResponseDTO(foundTipster.getId(), foundTipster.getName());

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<TipsterResponseDTO>> findAll(Authentication authentication, Pageable pageable){
        Bettor curentBettor = getBettor(authentication);

        Page<Tipster> tipsterPage = tipsterService.findAllByBettor(curentBettor, pageable);

        List<TipsterResponseDTO> tipstersDTO =
                tipsterPage.getContent().stream()
                        .map(tipsterMapper::toDto)
                        .toList();

        PageResponseDTO<TipsterResponseDTO> responseDTO = new PageResponseDTO<>(
                tipstersDTO,
                tipsterPage.getNumber(),
                tipsterPage.getSize(),
                tipsterPage.getTotalElements(),
                tipsterPage.getTotalPages()
        );

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    @PostMapping
    public ResponseEntity<TipsterResponseDTO> create(@Valid @RequestBody TipsterRequestDTO request, Authentication authentication){
        Bettor currentBettor = getBettor(authentication);

        Tipster createdTipster = tipsterService.create(request, currentBettor);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdTipster.getId()).toUri();

        TipsterResponseDTO responseDTO = tipsterMapper.toDto(createdTipster);

        return ResponseEntity.created(location).body(responseDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipsterResponseDTO> update(@PathVariable Long id,
                                                     @Valid @RequestBody TipsterRequestDTO request,
                                                     Authentication authentication){
        Bettor currentBettor = getBettor(authentication);

        Tipster updatedTipster = tipsterService.update(id, request, currentBettor);

        TipsterResponseDTO responseDTO = tipsterMapper.toDto(updatedTipster);

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    private Bettor getBettor(Authentication authentication){
        BettorDetails principal = (BettorDetails) authentication.getPrincipal();
        return principal.getBettor();
    }
}
