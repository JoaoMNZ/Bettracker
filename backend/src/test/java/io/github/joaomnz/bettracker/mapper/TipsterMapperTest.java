package io.github.joaomnz.bettracker.mapper;

import io.github.joaomnz.bettracker.dto.tipster.TipsterResponseDTO;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Tipster;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
public class TipsterMapperTest {
    @Autowired
    private TipsterMapper tipsterMapper;

    @Test
    @DisplayName("Should correctly map Tipster entity to TipsterResponseDTO")
    void toDtoShouldMapTipsterToTipsterResponseDTO() {
        Bettor owner = new Bettor();
        Tipster tipster = new Tipster(1L, "BestTipsterEver", owner);

        TipsterResponseDTO responseDTO = tipsterMapper.toDto(tipster);

        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.id()).isEqualTo(1L);
        assertThat(responseDTO.name()).isEqualTo("BestTipsterEver");
    }
}
