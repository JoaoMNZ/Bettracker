package io.github.joaomnz.bettracker.mappers;

import io.github.joaomnz.bettracker.dto.bet.CreateBetRequestDTO;
import io.github.joaomnz.bettracker.dto.bet.BetResponseDTO;
import io.github.joaomnz.bettracker.model.*;
import io.github.joaomnz.bettracker.model.enums.BetStatus;
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
public class BetMapperTest {
    @Autowired
    BetMapper betMapper;

    @Test
    void updateBetRequestToBet(){
        CreateBetRequestDTO request = new CreateBetRequestDTO(
                "Flamengo x Corinthians",
                "ML Flamengo",
                BigDecimal.TWO,
                null,
                BigDecimal.TEN,
                null,
                null,
                null,
                null,
                null,
                null);
        Bettor bettor = new Bettor();
        Bookmaker bookmaker = new Bookmaker(1L, "Bet365", bettor);
        Tipster tipster = new Tipster(1L, "Pei!", bettor);
        Sport sport = new Sport(1L, "Football", bettor);

        Bet bet = betMapper.toEntity(request, bookmaker, tipster, sport, null, bettor);
        assertThat(bet).isNotNull();
        System.out.println(bet);
    }

    @Test
    @DisplayName("Should correctly map Bet entity to BetResponseDTO when all associations are present")
    void shouldMapBetToBetResponseDTO_whenAllFieldsAreProvided() {
        Bettor bettor = new Bettor();

        Bookmaker bookmaker = new Bookmaker(1L, "Bet365", bettor);
        Sport sport = new Sport(2L, "Football", bettor);
        Competition competition = new Competition(3L, "Premier League", bettor, sport);
        Tipster tipster = new Tipster(4L, "Pei!", bettor);

        Bet bet = new Bet();
        bet.setId(100L);
        bet.setTitle("Test Bet");
        bet.setSelection("Home Win");
        bet.setStake(new BigDecimal("10.00"));
        bet.setOdds(new BigDecimal("1.85"));
        bet.setStatus(BetStatus.WON);
        bet.setEventDate(LocalDateTime.now());
        bet.setBookmaker(bookmaker);
        bet.setSport(sport);
        bet.setCompetition(competition);
        bet.setTipster(tipster);

        BetResponseDTO responseDTO = betMapper.toDto(bet);

        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.id()).isEqualTo(100L);
        assertThat(responseDTO.title()).isEqualTo("Test Bet");
        assertThat(responseDTO.status()).isEqualTo(BetStatus.WON);

        assertThat(responseDTO.bookmaker()).isNotNull();
        assertThat(responseDTO.bookmaker()).isEqualTo("Bet365");

        assertThat(responseDTO.sport()).isNotNull();
        assertThat(responseDTO.sport()).isEqualTo("Football");

        assertThat(responseDTO.competition()).isNotNull();
        assertThat(responseDTO.competition()).isEqualTo("Premier League");
        assertThat(responseDTO.tipster()).isNotNull();

        assertThat(responseDTO.tipster()).isEqualTo("Pei!");
    }
}
