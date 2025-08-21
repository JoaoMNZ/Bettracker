package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.dto.bookmaker.BookmakerRequestDTO;
import io.github.joaomnz.bettracker.dto.bookmaker.BookmakerResponseDTO;
import io.github.joaomnz.bettracker.dto.shared.PageResponseDTO;
import io.github.joaomnz.bettracker.mapper.BookmakerMapper;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Bookmaker;
import io.github.joaomnz.bettracker.security.BettorDetails;
import io.github.joaomnz.bettracker.service.BookmakerService;
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

@RequestMapping("/api/v1/bookmakers")
@RestController
public class BookmakerController {
    private final BookmakerService bookmakerService;
    private final BookmakerMapper bookmakerMapper;

    public BookmakerController(BookmakerService bookmakerService, BookmakerMapper bookmakerMapper) {
        this.bookmakerService = bookmakerService;
        this.bookmakerMapper = bookmakerMapper;
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookmakerResponseDTO> findById(@PathVariable Long id, Authentication authentication){
        Bettor currentBettor = getBettor(authentication);

        Bookmaker foundBookmaker = bookmakerService.findByIdAndBettor(id, currentBettor);

        BookmakerResponseDTO responseDTO = bookmakerMapper.toDto(foundBookmaker);

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<BookmakerResponseDTO>> findAll(Authentication authentication, Pageable pageable){
        Bettor currentBettor = getBettor(authentication);

        Page<Bookmaker> bookmakerPage = bookmakerService.findAllByBettor(currentBettor, pageable);

        List<BookmakerResponseDTO> bookmakersDTO = bookmakerPage.getContent().stream()
                .map(bookmakerMapper::toDto)
                .toList();

        PageResponseDTO<BookmakerResponseDTO> responseDTO = new PageResponseDTO<>(
                bookmakersDTO,
                bookmakerPage.getNumber(),
                bookmakerPage.getSize(),
                bookmakerPage.getTotalElements(),
                bookmakerPage.getTotalPages()
        );

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
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

    private Bettor getBettor(Authentication authentication){
        BettorDetails principal = (BettorDetails) authentication.getPrincipal();
        return principal.getBettor();
    }
}
