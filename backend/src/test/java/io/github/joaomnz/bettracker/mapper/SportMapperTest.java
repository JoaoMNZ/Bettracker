package io.github.joaomnz.bettracker.mapper;

import io.github.joaomnz.bettracker.dto.sport.SportResponseDTO;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Sport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class SportMapperTest {
    @Autowired
    private SportMapper sportMapper;

    @Test
    @DisplayName("Should correctly map Sport entity to SportResponseDTO")
    void toDtoShouldMapSportToSportResponseDTO() {
        Bettor owner = new Bettor();
        Sport sport = new Sport(1L, "Football", owner);

        SportResponseDTO responseDTO = sportMapper.toDto(sport);

        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.id()).isEqualTo(1L);
        assertThat(responseDTO.name()).isEqualTo("Football");
    }
}
