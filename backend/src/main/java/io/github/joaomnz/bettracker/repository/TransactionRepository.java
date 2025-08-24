package io.github.joaomnz.bettracker.repository;

import io.github.joaomnz.bettracker.model.Bookmaker;
import io.github.joaomnz.bettracker.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByIdAndBookmaker(Long id, Bookmaker bookmaker);
    Page<Transaction> findAllByBookmaker(Bookmaker bookmaker, Pageable pageable);
}
