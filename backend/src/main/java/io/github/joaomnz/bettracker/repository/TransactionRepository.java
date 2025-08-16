package io.github.joaomnz.bettracker.repository;

import io.github.joaomnz.bettracker.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
