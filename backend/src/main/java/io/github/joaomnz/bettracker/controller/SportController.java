package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.dto.shared.PageResponseDTO;
import io.github.joaomnz.bettracker.dto.sport.SportRequestDTO;
import io.github.joaomnz.bettracker.dto.sport.SportResponseDTO;
import io.github.joaomnz.bettracker.mapper.SportMapper;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Sport;
import io.github.joaomnz.bettracker.security.BettorDetails;
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

@RequestMapping("/api/v1/sports")
@RestController
public class SportController {
    private final SportService sportService;
    private final SportMapper sportMapper;


    public SportController(SportService sportService, SportMapper sportMapper) {
        this.sportService = sportService;
        this.sportMapper = sportMapper;
    }

    @GetMapping("/{id}")
    public ResponseEntity<SportResponseDTO> findById(@PathVariable Long id, Authentication authentication){
        Bettor currentBettor = getBettor(authentication);

        Sport foundSport = sportService.findByIdAndBettor(id, currentBettor);

        SportResponseDTO responseDTO = sportMapper.toDto(foundSport);

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<SportResponseDTO>> findAll(Authentication authentication, Pageable pageable){
        Bettor currentBettor = getBettor(authentication);

        Page<Sport> sportPage = sportService.findAllByBettor(currentBettor, pageable);

        List<SportResponseDTO> sportsDTO = sportPage.getContent().stream()
                .map(sportMapper::toDto)
                .toList();

        PageResponseDTO<SportResponseDTO> responseDTO = new PageResponseDTO<>(
                sportsDTO,
                sportPage.getNumber(),
                sportPage.getSize(),
                sportPage.getTotalElements(),
                sportPage.getTotalPages()
        );

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    @PostMapping
    public ResponseEntity<SportResponseDTO> create(@Valid @RequestBody SportRequestDTO request, Authentication authentication){
        BettorDetails principal = (BettorDetails) authentication.getPrincipal();
        Bettor currentBettor = principal.getBettor();
        Sport createdSport = sportService.create(request, currentBettor);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdSport.getId())
                .toUri();

        SportResponseDTO responseDTO = new SportResponseDTO(createdSport.getId(), createdSport.getName());
        return ResponseEntity.created(location).body(responseDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SportResponseDTO> update(@PathVariable Long id,
                                                   @Valid @RequestBody SportRequestDTO request,
                                                   Authentication authentication){
        Bettor currentBettor = getBettor(authentication);

        Sport updatedSport = sportService.update(id, request, currentBettor);

        SportResponseDTO responseDTO = sportMapper.toDto(updatedSport);

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    private Bettor getBettor(Authentication authentication){
        BettorDetails principal = (BettorDetails) authentication.getPrincipal();
        return principal.getBettor();
    }
}
