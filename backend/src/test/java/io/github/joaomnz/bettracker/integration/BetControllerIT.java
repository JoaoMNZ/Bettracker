package io.github.joaomnz.bettracker.integration;

import io.github.joaomnz.bettracker.dto.auth.LoginResponseDTO;
import io.github.joaomnz.bettracker.dto.auth.RegisterRequestDTO;
import io.github.joaomnz.bettracker.dto.bet.CreateBetRequestDTO;
import io.github.joaomnz.bettracker.dto.bet.CreateBetResponseDTO;
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
    @DisplayName("Should create a bet when authenticated and bookmaker belongs to the user")
    void shouldCreateBetWhenAuthenticatedAndBookmakerIsValid() {
        String token = registerUserAndGetToken("joao.menezes21@Outlook.com");
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

        ResponseEntity<CreateBetResponseDTO> response = testRestTemplate
                .exchange(BETS_API_URI, HttpMethod.POST, httpEntity, CreateBetResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().selection()).isEqualTo("ML Vasco");
        assertThat(response.getBody().bookmaker()).isEqualTo("Bet365");
        assertThat(response.getBody().tipster()).isEqualTo("Pei!");
        assertThat(response.getBody().sport()).isEqualTo("Football");
        assertThat(response.getBody().competition()).isEqualTo("Premier League");
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to create a bet with a bookmaker from another user")
    void shouldReturnNotFoundWhenBookmakerBelongsToAnotherUser() {
        String tokenUserA = registerUserAndGetToken("userA@email.com");
        Long bookmakerIdUserA = createBookmaker(tokenUserA, "Bookmaker of A");

        String tokenUserB = registerUserAndGetToken("userB@email.com");

        CreateBetRequestDTO request = new CreateBetRequestDTO(
                "Invalid Bet", "Any selection", BigDecimal.TEN, StakeType.VALUE, BigDecimal.TEN,
                null, null, bookmakerIdUserA, null, null, null
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenUserB);
        HttpEntity<CreateBetRequestDTO> httpEntity = new HttpEntity<>(request, headers);

        ResponseEntity<ErrorResponseDTO> response = testRestTemplate
                .exchange(BETS_API_URI, HttpMethod.POST, httpEntity, ErrorResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Bookmaker not found with id " + bookmakerIdUserA + " for this bettor.");
    }

    private String registerUserAndGetToken(String email) {
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
}
