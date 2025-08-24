package io.github.joaomnz.bettracker.mapper;

import io.github.joaomnz.bettracker.dto.transaction.TransactionResponseDTO;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Bookmaker;
import io.github.joaomnz.bettracker.model.Transaction;
import io.github.joaomnz.bettracker.model.enums.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class TransactionMapperTest {

    @Autowired
    private TransactionMapper transactionMapper;

    @Test
    @DisplayName("Should correctly map Transaction entity to TransactionResponseDTO")
    void toDtoShouldMapTransactionToTransactionResponseDTO() {
        Bettor owner = new Bettor();
        Bookmaker parentBookmaker = new Bookmaker(1L, "Bet365", owner);
        Transaction transaction = new Transaction();
        transaction.setId(50L);
        transaction.setAmount(new BigDecimal("150.75"));
        transaction.setType(TransactionType.DEPOSIT);

        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setBettor(owner);
        transaction.setBookmaker(parentBookmaker);

        TransactionResponseDTO responseDTO = transactionMapper.toDto(transaction);

        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.id()).isEqualTo(50L);
        assertThat(responseDTO.amount()).isEqualTo(new BigDecimal("150.75"));
        assertThat(responseDTO.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(responseDTO.createdAt()).isEqualTo(transaction.getCreatedAt());
    }
}