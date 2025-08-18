package io.github.joaomnz.bettracker.integration;

import io.github.joaomnz.bettracker.dto.auth.LoginResponseDTO;
import io.github.joaomnz.bettracker.dto.auth.RegisterRequestDTO;
import io.github.joaomnz.bettracker.dto.bet.CreateBetRequestDTO;
import io.github.joaomnz.bettracker.dto.bet.BetResponseDTO;
import io.github.joaomnz.bettracker.dto.bookmaker.BookmakerRequestDTO;
import io.github.joaomnz.bettracker.dto.bookmaker.BookmakerResponseDTO;
import io.github.joaomnz.bettracker.dto.competition.CompetitionRequestDTO;
import io.github.joaomnz.bettracker.dto.competition.CompetitionResponseDTO;
import io.github.joaomnz.bettracker.dto.shared.ErrorResponseDTO;
import io.github.joaomnz.bettracker.dto.sport.SportRequestDTO;
import io.github.joaomnz.bettracker.dto.sport.SportResponseDTO;
import io.github.joaomnz.bettracker.dto.tipster.TipsterRequestDTO;
import io.github.joaomnz.bettracker.dto.tipster.TipsterResponseDTO;
import io.github.joaomnz.bettracker.model.enums.StakeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BetControllerIT {
    @Autowired
    private TestRestTemplate testRestTemplate;

    private final String BOOKMAKERS_API_URI = "/api/v1/bookmakers";
    private final String TIPSTERS_API_URI = "/api/v1/tipsters";
    private final String SPORTS_API_URI = "/api/v1/sports";
    private final String BETS_API_URI = "/api/v1/bets";

    @Test
    @DisplayName("Should create a bet when authenticated and bookmaker belongs to the bettor")
    void shouldCreateBetWhenAuthenticatedAndBookmakerIsValid() {
        String token = registerBettorAndGetToken("joao.menezes21@Outlook.com");
        Long bookmakerId = createBookmaker(token, "Bet365");
        Long tipsterId = createTipster(token, "Pei!");
        Long sportId = createSport(token, "Football");
        Long competitionId = createCompetition(token, sportId, "Premier League");

        CreateBetRequestDTO request = new CreateBetRequestDTO(
                "Vasco x Botafogo",
                "ML Vasco",
                BigDecimal.TWO,
                StakeType.UNIT,
                BigDecimal.valueOf(2.5),
                null,
                null,
                bookmakerId,
                tipsterId,
                sportId,
                competitionId
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<CreateBetRequestDTO> httpEntity = new HttpEntity<>(request, headers);

        ResponseEntity<BetResponseDTO> response = testRestTemplate
                .exchange(BETS_API_URI, HttpMethod.POST, httpEntity, BetResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().selection()).isEqualTo("ML Vasco");
        assertThat(response.getBody().bookmaker()).isEqualTo("Bet365");
        assertThat(response.getBody().tipster()).isEqualTo("Pei!");
        assertThat(response.getBody().sport()).isEqualTo("Football");
        assertThat(response.getBody().competition()).isEqualTo("Premier League");
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to create a bet with a bookmaker from another bettor")
    void shouldReturnNotFoundWhenBookmakerBelongsToAnotherBettor() {
        String tokenBettorA = registerBettorAndGetToken("userA@email.com");
        Long bookmakerIdBettorA = createBookmaker(tokenBettorA, "Bookmaker of A");

        String tokenBettorB = registerBettorAndGetToken("userB@email.com");

        CreateBetRequestDTO request = new CreateBetRequestDTO(
                "Invalid Bet", "Any selection", BigDecimal.TEN, StakeType.VALUE, BigDecimal.TEN,
                null, null, bookmakerIdBettorA, null, null, null
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenBettorB);
        HttpEntity<CreateBetRequestDTO> httpEntity = new HttpEntity<>(request, headers);

        ResponseEntity<ErrorResponseDTO> response = testRestTemplate
                .exchange(BETS_API_URI, HttpMethod.POST, httpEntity, ErrorResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Bookmaker not found with id " + bookmakerIdBettorA + " for this bettor.");
    }

    @Test
    @DisplayName("Should delete a bet when authenticated bettor is the owner")
    void shouldDeleteBetWhenBettorIsOwner() {
        String token = registerBettorAndGetToken("user.to.delete@email.com");
        Long betId = createSimpleBetAndGetId(token);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<Void> response = testRestTemplate.exchange(
                BETS_API_URI + "/" + betId,
                HttpMethod.DELETE,
                httpEntity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to delete a bet from another bettor")
    void shouldReturnNotFound_whenDeletingBetFromAnotherBettor() {
        String tokenBettorA = registerBettorAndGetToken("userA.delete@email.com");
        Long betIdBettorA = createSimpleBetAndGetId(tokenBettorA);

        String tokenBettorB = registerBettorAndGetToken("userB.delete@email.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenBettorB);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponseDTO> response = testRestTemplate.exchange(
                BETS_API_URI + "/" + betIdBettorA,
                HttpMethod.DELETE,
                httpEntity,
                ErrorResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Bet not found with id " + betIdBettorA + " for this bettor.");
    }

    private String registerBettorAndGetToken(String email) {
        RegisterRequestDTO bettor = new RegisterRequestDTO("Test User", email, "Password123!");
        ResponseEntity<LoginResponseDTO> response =
                testRestTemplate.postForEntity("/api/v1/users/register", bettor, LoginResponseDTO.class);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().token();
    }

    private Long createBookmaker(String token, String bookmakerName){
        BookmakerRequestDTO request = new BookmakerRequestDTO(bookmakerName);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<BookmakerRequestDTO> httpEntity = new HttpEntity<>(request, headers);

        ResponseEntity<BookmakerResponseDTO> response = testRestTemplate
                .exchange(BOOKMAKERS_API_URI, HttpMethod.POST, httpEntity, BookmakerResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().id();
    }

    private Long createTipster(String token, String tipsterName){
        TipsterRequestDTO request = new TipsterRequestDTO(tipsterName);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<TipsterRequestDTO> httpEntity = new HttpEntity<>(request, headers);

        ResponseEntity<TipsterResponseDTO> response = testRestTemplate
                .exchange(TIPSTERS_API_URI, HttpMethod.POST, httpEntity, TipsterResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().id();
    }

    private Long createSport(String token, String sportName){
        SportRequestDTO request = new SportRequestDTO(sportName);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<SportRequestDTO> httpEntity = new HttpEntity<>(request, headers);

        ResponseEntity<SportResponseDTO> response = testRestTemplate
                .exchange(SPORTS_API_URI, HttpMethod.POST, httpEntity, SportResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().id();
    }

    private Long createCompetition(String token, Long sportId, String competitionName){
        CompetitionRequestDTO request = new CompetitionRequestDTO(competitionName);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<CompetitionRequestDTO> httpEntity = new HttpEntity<>(request, headers);

        ResponseEntity<CompetitionResponseDTO> response = testRestTemplate
                .exchange(SPORTS_API_URI + "/" + sportId + "/competitions", HttpMethod.POST, httpEntity, CompetitionResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().id();
    }

    private Long createSimpleBetAndGetId(String token) {
        CreateBetRequestDTO request = new CreateBetRequestDTO(
                "Simple Bet", "Any Selection", new BigDecimal("10"), StakeType.VALUE,
                new BigDecimal("1.5"), null, null, null, null, null, null
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<CreateBetRequestDTO> httpEntity = new HttpEntity<>(request, headers);
        ResponseEntity<BetResponseDTO> response = testRestTemplate.exchange(
                BETS_API_URI, HttpMethod.POST, httpEntity, BetResponseDTO.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().id();
    }
}
