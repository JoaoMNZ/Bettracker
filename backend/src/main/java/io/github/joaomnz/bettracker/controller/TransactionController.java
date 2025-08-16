package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.dto.transaction.TransactionRequestDTO;
import io.github.joaomnz.bettracker.dto.transaction.TransactionResponseDTO;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Bookmaker;
import io.github.joaomnz.bettracker.model.Transaction;
import io.github.joaomnz.bettracker.security.BettorDetails;
import io.github.joaomnz.bettracker.service.BookmakerService;
import io.github.joaomnz.bettracker.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RequestMapping("/api/v1/bookmakers/{bookmakerId}/transactions")
@RestController
public class TransactionController {
    private final TransactionService transactionService;
    private final BookmakerService bookmakerService;

    public TransactionController(TransactionService transactionService, BookmakerService bookmakerService) {
        this.transactionService = transactionService;
        this.bookmakerService = bookmakerService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponseDTO> create(@PathVariable Long bookmakerId,
                                                         @Valid @RequestBody TransactionRequestDTO request,
                                                         Authentication authentication){
        BettorDetails principal = (BettorDetails) authentication.getPrincipal();
        Bettor currentBettor = principal.getBettor();
        Bookmaker parentBookmaker = bookmakerService.findByIdAndBettor(bookmakerId, currentBettor);
        Transaction createdTransaction = transactionService.create(request, parentBookmaker);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdTransaction.getId())
                .toUri();

        TransactionResponseDTO responseDTO = new TransactionResponseDTO(
                createdTransaction.getId(),
                createdTransaction.getAmount(),
                createdTransaction.getType(),
                createdTransaction.getCreatedAt());
        return ResponseEntity.created(location).body(responseDTO);
    }
}
