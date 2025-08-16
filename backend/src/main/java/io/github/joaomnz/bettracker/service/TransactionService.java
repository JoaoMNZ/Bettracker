package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.transaction.TransactionRequestDTO;
import io.github.joaomnz.bettracker.model.Bookmaker;
import io.github.joaomnz.bettracker.model.Transaction;
import io.github.joaomnz.bettracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction create(TransactionRequestDTO request, Bookmaker parentBookmaker){
        Transaction newTransaction = new Transaction();
        newTransaction.setAmount(request.amount());
        newTransaction.setType(request.type());
        newTransaction.setBookmaker(parentBookmaker);
        newTransaction.setBettor(parentBookmaker.getBettor());
        return  transactionRepository.save(newTransaction);
    }
}
