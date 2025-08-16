package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.dto.bookmaker.BookmakerRequestDTO;
import io.github.joaomnz.bettracker.dto.bookmaker.BookmakerResponseDTO;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Bookmaker;
import io.github.joaomnz.bettracker.security.BettorDetails;
import io.github.joaomnz.bettracker.service.BookmakerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RequestMapping("/api/v1/bookmakers")
@RestController
public class BookmakerController {
    private final BookmakerService bookmakerService;

    public BookmakerController(BookmakerService bookmakerService) {
        this.bookmakerService = bookmakerService;
    }

    @PostMapping
    public ResponseEntity<BookmakerResponseDTO> create(@Valid @RequestBody BookmakerRequestDTO request, Authentication authentication){
        BettorDetails principal = (BettorDetails) authentication.getPrincipal();
        Bettor currentBettor = principal.getBettor();
        Bookmaker createdBookmaker = bookmakerService.create(request, currentBettor);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(createdBookmaker.getId())
                .toUri();

        BookmakerResponseDTO responseDTO = new BookmakerResponseDTO(createdBookmaker.getId(), createdBookmaker.getName());
        return ResponseEntity.created(location).body(responseDTO);
    }
}
