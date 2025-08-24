package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.transaction.TransactionRequestDTO;
import io.github.joaomnz.bettracker.exceptions.ResourceNotFoundException;
import io.github.joaomnz.bettracker.model.Bookmaker;
import io.github.joaomnz.bettracker.model.Transaction;
import io.github.joaomnz.bettracker.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction findByIdAndBookmaker(Long id, Bookmaker parentBookmaker){
        return transactionRepository.findByIdAndBookmaker(id, parentBookmaker)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id " + id + " for this bookmaker."));
    }

    public Page<Transaction> findAllByBookmaker(Bookmaker parentBookmaker, Pageable pageable){
        return transactionRepository.findAllByBookmaker(parentBookmaker, pageable);
    }

    public Transaction create(TransactionRequestDTO request, Bookmaker parentBookmaker){
        Transaction newTransaction = new Transaction();
        newTransaction.setAmount(request.amount());
        newTransaction.setType(request.type());
        newTransaction.setBookmaker(parentBookmaker);
        newTransaction.setBettor(parentBookmaker.getBettor());
        return  transactionRepository.save(newTransaction);
    }

    public void delete(Long transactionId, Bookmaker bookmaker){
        Transaction transactionToDelete = findByIdAndBookmaker(transactionId, bookmaker);

        transactionRepository.delete(transactionToDelete);
    }
}
