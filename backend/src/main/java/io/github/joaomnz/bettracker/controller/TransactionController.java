package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.dto.shared.PageResponseDTO;
import io.github.joaomnz.bettracker.dto.transaction.TransactionRequestDTO;
import io.github.joaomnz.bettracker.dto.transaction.TransactionResponseDTO;
import io.github.joaomnz.bettracker.dto.transaction.UpdateTransactionRequestDTO;
import io.github.joaomnz.bettracker.mapper.TransactionMapper;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Bookmaker;
import io.github.joaomnz.bettracker.model.Transaction;
import io.github.joaomnz.bettracker.security.BettorDetails;
import io.github.joaomnz.bettracker.service.BookmakerService;
import io.github.joaomnz.bettracker.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RequestMapping("/api/v1/bookmakers/{bookmakerId}/transactions")
@RestController
public class TransactionController {
    private final TransactionService transactionService;
    private final BookmakerService bookmakerService;
    private final TransactionMapper transactionMapper;

    public TransactionController(TransactionService transactionService, BookmakerService bookmakerService, TransactionMapper transactionMapper) {
        this.transactionService = transactionService;
        this.bookmakerService = bookmakerService;
        this.transactionMapper = transactionMapper;
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDTO> findById(@PathVariable Long bookmakerId,
                                                           @PathVariable Long transactionId,
                                                           Authentication authentication){
        Bettor currentBettor = getBettor(authentication);

        Bookmaker parentBookmaker = bookmakerService.findByIdAndBettor(bookmakerId, currentBettor);
        Transaction foundTransaction = transactionService.findByIdAndBookmaker(transactionId, parentBookmaker);

        TransactionResponseDTO responseDTO = transactionMapper.toDto(foundTransaction);

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<TransactionResponseDTO>> findAll(@PathVariable Long bookmakerId,
                                                                           Authentication authentication,
                                                                           Pageable pageable){
        Bettor currentBettor = getBettor(authentication);

        Bookmaker parentBookmaker = bookmakerService.findByIdAndBettor(bookmakerId, currentBettor);
        Page<Transaction> transactionPage = transactionService.findAllByBookmaker(parentBookmaker, pageable);

        List<TransactionResponseDTO> transactionsDTO = transactionPage.getContent().stream()
                .map(transactionMapper::toDto)
                .toList();

        PageResponseDTO<TransactionResponseDTO> responseDTO = new PageResponseDTO<>(
                transactionsDTO,
                transactionPage.getNumber(),
                transactionPage.getSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages()
        );

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
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

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> delete(@PathVariable Long bookmakerId,
                                       @PathVariable Long transactionId,
                                       Authentication authentication){
        Bettor currentBettor = getBettor(authentication);

        Bookmaker parentBookmaker = bookmakerService.findByIdAndBettor(bookmakerId, currentBettor);
        transactionService.delete(transactionId, parentBookmaker);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDTO> update(@PathVariable Long bookmakerId,
                                                         @PathVariable Long transactionId,
                                                         @Valid @RequestBody UpdateTransactionRequestDTO request,
                                                         Authentication authentication){
        Bettor currentBettor = getBettor(authentication);

        Bookmaker parentBookmaker = bookmakerService.findByIdAndBettor(bookmakerId, currentBettor);
        Transaction updatedTransaction = transactionService.update(transactionId, request, parentBookmaker);

        TransactionResponseDTO responseDTO = transactionMapper.toDto(updatedTransaction);

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    public Bettor getBettor(Authentication authentication){
        BettorDetails principal = (BettorDetails) authentication.getPrincipal();
        return principal.getBettor();
    }
}
