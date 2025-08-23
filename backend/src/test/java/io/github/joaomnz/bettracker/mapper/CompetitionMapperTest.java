package io.github.joaomnz.bettracker.mapper;

import io.github.joaomnz.bettracker.dto.competition.CompetitionResponseDTO;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Competition;
import io.github.joaomnz.bettracker.model.Sport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class CompetitionMapperTest {
    @Autowired
    private CompetitionMapper competitionMapper;

    @Test
    @DisplayName("Should correctly map Competition entity to CompetitionResponseDTO")
    void toDtoShouldMapCompetitionToCompetitionResponseDTO() {
        Bettor owner = new Bettor();
        Sport parentSport = new Sport(1L, "Football", owner);
        Competition competition = new Competition(10L, "Premier League", owner, parentSport);

        CompetitionResponseDTO responseDTO = competitionMapper.toDto(competition);

        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.id()).isEqualTo(10L);
        assertThat(responseDTO.name()).isEqualTo("Premier League");
    }
}