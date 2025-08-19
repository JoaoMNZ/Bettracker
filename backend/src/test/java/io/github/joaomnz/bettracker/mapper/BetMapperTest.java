package io.github.joaomnz.bettracker.mapper;

import io.github.joaomnz.bettracker.dto.bet.BetResponseDTO;
import io.github.joaomnz.bettracker.dto.bet.CreateBetRequestDTO;
import io.github.joaomnz.bettracker.model.*;
import io.github.joaomnz.bettracker.model.enums.BetStatus;
import io.github.joaomnz.bettracker.model.enums.StakeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class BetMapperTest {

    @Autowired
    private BetMapper betMapper;

    @Nested
    @DisplayName("Tests for toEntity (DTO -> Entity) method")
    class ToEntityMapping {

        @Test
        @DisplayName("Should map DTO to entity and apply default values when optional fields are null")
        void toEntityShouldMapDtoAndApplyDefaultsWhenOptionalFieldsAreNull() {
            CreateBetRequestDTO requestDTO = new CreateBetRequestDTO(
                    "Flamengo x Corinthians", "ML Flamengo", BigDecimal.TEN,
                    null, BigDecimal.valueOf(1.8), null, null,
                    1L, 2L, 3L, 4L
            );
            Bettor owner = new Bettor();
            Bookmaker bookmaker = new Bookmaker();

            Bet resultBet = betMapper.toEntity(requestDTO, bookmaker, null, null, null, owner);

            assertThat(resultBet).isNotNull();
            assertThat(resultBet.getSelection()).isEqualTo("ML Flamengo");
            assertThat(resultBet.getBookmaker()).isEqualTo(bookmaker);
            assertThat(resultBet.getBettor()).isEqualTo(owner);

            assertThat(resultBet.getStatus()).isEqualTo(BetStatus.PENDING);
            assertThat(resultBet.getStakeType()).isEqualTo(StakeType.VALUE);
            assertThat(resultBet.getEventDate()).isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("Should map DTO to entity and use provided values instead of defaults")
        void toEntityShouldMapDtoAndUseProvidedValues() {
            CreateBetRequestDTO requestDTO = new CreateBetRequestDTO(
                    "Palmeiras x São Paulo", "Over 2.5", BigDecimal.TEN,
                    StakeType.UNIT, BigDecimal.valueOf(2.1), BetStatus.WON, LocalDateTime.of(2025, 10, 5, 21, 0),
                    1L, null, null, null
            );
            Bettor owner = new Bettor();

            Bet resultBet = betMapper.toEntity(requestDTO, null, null, null, null, owner);

            assertThat(resultBet.getStatus()).isEqualTo(BetStatus.WON);
            assertThat(resultBet.getStakeType()).isEqualTo(StakeType.UNIT);
            assertThat(resultBet.getEventDate()).isEqualTo(LocalDateTime.of(2025, 10, 5, 21, 0));
        }
    }

    @Nested
    @DisplayName("Tests for toDto (Entity -> DTO) method")
    class ToDtoMapping {

        @Test
        @DisplayName("Should map entity to DTO correctly when all associations are present")
        void toDtoShouldMapEntityWhenAllAssociationsArePresent() {
            Bettor bettor = new Bettor();
            Bookmaker bookmaker = new Bookmaker(1L, "Bet365", bettor);
            Sport sport = new Sport(2L, "Football", bettor);
            Competition competition = new Competition(3L, "Premier League", bettor, sport);
            Bet bet = new Bet();
            bet.setId(100L);
            bet.setSelection("Home Win");
            bet.setBookmaker(bookmaker);
            bet.setCompetition(competition);

            BetResponseDTO responseDTO = betMapper.toDto(bet);

            assertThat(responseDTO).isNotNull();
            assertThat(responseDTO.id()).isEqualTo(100L);
            assertThat(responseDTO.selection()).isEqualTo("Home Win");
            assertThat(responseDTO.bookmaker()).isEqualTo("Bet365");
            assertThat(responseDTO.competition()).isEqualTo("Premier League");
        }

        @Test
        @DisplayName("Should map entity to DTO correctly when associations are null")
        void toDtoShouldMapEntityWhenAssociationsAreNull() {
            Bet bet = new Bet();
            bet.setId(101L);
            bet.setSelection("Draw");
            bet.setBookmaker(null);
            bet.setCompetition(null);

            BetResponseDTO responseDTO = betMapper.toDto(bet);

            assertThat(responseDTO).isNotNull();
            assertThat(responseDTO.id()).isEqualTo(101L);
            assertThat(responseDTO.bookmaker()).isNull();
            assertThat(responseDTO.competition()).isNull();
        }
    }
}