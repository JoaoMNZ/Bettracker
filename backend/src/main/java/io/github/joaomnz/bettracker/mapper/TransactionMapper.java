package io.github.joaomnz.bettracker.mapper;

import io.github.joaomnz.bettracker.dto.transaction.TransactionResponseDTO;
import io.github.joaomnz.bettracker.model.Transaction;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    TransactionResponseDTO toDto(Transaction transaction);
}
