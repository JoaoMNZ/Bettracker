package io.github.joaomnz.bettracker.mapper;

import io.github.joaomnz.bettracker.dto.bookmaker.BookmakerResponseDTO;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Bookmaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class BookmakerMapperTest {
    @Autowired
    private BookmakerMapper bookmakerMapper;

    @Test
    @DisplayName("Should correctly map Bookmaker entity to BookmakerResponseDTO")
    void toDtoShouldMapBookmakerToBookmakerResponseDTO() {
        Bettor owner = new Bettor();
        Bookmaker bookmaker = new Bookmaker(1L, "Bet365", owner);

        BookmakerResponseDTO responseDTO = bookmakerMapper.toDto(bookmaker);

        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.id()).isEqualTo(1L);
        assertThat(responseDTO.name()).isEqualTo("Bet365");
    }
}