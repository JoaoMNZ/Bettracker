package io.github.joaomnz.bettracker.integration;

import io.github.joaomnz.bettracker.dto.auth.LoginResponseDTO;
import io.github.joaomnz.bettracker.dto.auth.RegisterRequestDTO;
import io.github.joaomnz.bettracker.dto.competition.CompetitionRequestDTO;
import io.github.joaomnz.bettracker.dto.competition.CompetitionResponseDTO;
import io.github.joaomnz.bettracker.dto.shared.ErrorResponseDTO;
import io.github.joaomnz.bettracker.dto.shared.PageResponseDTO;
import io.github.joaomnz.bettracker.dto.sport.SportRequestDTO;
import io.github.joaomnz.bettracker.dto.sport.SportResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CompetitionControllerIT {

    @Autowired
    private TestRestTemplate testRestTemplate;

    private final String SPORTS_API_URI = "/api/v1/sports";

    @Test
    @DisplayName("Should return a single competition when bettor is the owner")
    void findByIdWhenBettorIsOwner() {
        AuthContext authContext = registerUserAndCreateSport("user.get.one@email.com", "Football");
        Long competitionId = createSimpleCompetition(authContext, "Premier League");
        String competitionApiUri = SPORTS_API_URI + "/" + authContext.sportId() + "/competitions/" + competitionId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authContext.token());
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<CompetitionResponseDTO> response = testRestTemplate.exchange(
                competitionApiUri,
                HttpMethod.GET,
                httpEntity,
                CompetitionResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(competitionId);
        assertThat(response.getBody().name()).isEqualTo("Premier League");
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to get a competition from another bettor's sport")
    void findByIdWhenBettorIsNotOwner() {
        AuthContext userA = registerUserAndCreateSport("userA.get@email.com", "Football");
        Long competitionIdUserA = createSimpleCompetition(userA, "La Liga");

        String tokenUserB = registerUserAndGetToken("userB.get@email.com");
        String competitionApiUri = SPORTS_API_URI + "/" + userA.sportId() + "/competitions/" + competitionIdUserA;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenUserB);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponseDTO> response = testRestTemplate.exchange(
                competitionApiUri,
                HttpMethod.GET,
                httpEntity,
                ErrorResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should return a paginated list of competitions for a given sport")
    void findAllWhenAuthenticated() {
        AuthContext authContext = registerUserAndCreateSport("user.get.all@email.com", "Tennis");
        createSimpleCompetition(authContext, "Wimbledon");
        createSimpleCompetition(authContext, "Roland Garros");
        String competitionApiUri = SPORTS_API_URI + "/" + authContext.sportId() + "/competitions";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authContext.token());
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ParameterizedTypeReference<PageResponseDTO<CompetitionResponseDTO>> responseType = new ParameterizedTypeReference<>() {};

        ResponseEntity<PageResponseDTO<CompetitionResponseDTO>> response = testRestTemplate.exchange(
                competitionApiUri,
                HttpMethod.GET,
                httpEntity,
                responseType
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(2);
        assertThat(response.getBody().totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should create a competition when authenticated and sport belongs to the user")
    void shouldCreateCompetitionWhenAuthenticatedAndSportIsValid() {
        AuthContext authContext = registerUserAndCreateSport("user1@email.com", "Football");
        String competitionApiUri = SPORTS_API_URI + "/" + authContext.sportId() + "/competitions";

        CompetitionRequestDTO newCompetitionRequest = new CompetitionRequestDTO("Premier League");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authContext.token());
        HttpEntity<CompetitionRequestDTO> requestEntity = new HttpEntity<>(newCompetitionRequest, headers);

        ResponseEntity<CompetitionResponseDTO> response = testRestTemplate.exchange(
                competitionApiUri,
                HttpMethod.POST,
                requestEntity,
                CompetitionResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Premier League");
    }

    @Test
    @DisplayName("Should return 403 Forbidden when trying to create a competition without authentication")
    void shouldReturnForbiddenWhenNotAuthenticated() {
        AuthContext authContext = registerUserAndCreateSport("user2@email.com", "Basketball");
        String competitionApiUri = SPORTS_API_URI + "/" + authContext.sportId() + "/competitions";

        CompetitionRequestDTO newCompetitionRequest = new CompetitionRequestDTO("NBA");
        HttpEntity<CompetitionRequestDTO> requestEntity = new HttpEntity<>(newCompetitionRequest);

        ResponseEntity<String> response = testRestTemplate.exchange(
                competitionApiUri,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to create a competition for a non-existent sport")
    void shouldReturnNotFoundWhenSportDoesNotExist() {
        String token = registerUserAndGetToken("user3@email.com");
        long nonExistentSportId = 999L;
        String competitionApiUri = SPORTS_API_URI + "/" + nonExistentSportId + "/competitions";

        CompetitionRequestDTO newCompetitionRequest = new CompetitionRequestDTO("NFL");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<CompetitionRequestDTO> requestEntity = new HttpEntity<>(newCompetitionRequest, headers);

        ResponseEntity<String> response = testRestTemplate.exchange(
                competitionApiUri,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to create a competition for a sport that belongs to another user")
    void shouldReturnNotFoundWhenSportBelongsToAnotherUser() {
        AuthContext userA = registerUserAndCreateSport("userA@email.com", "Formula 1");

        String tokenUserB = registerUserAndGetToken("userB@email.com");

        String competitionApiUri = SPORTS_API_URI + "/" + userA.sportId() + "/competitions";
        CompetitionRequestDTO newCompetitionRequest = new CompetitionRequestDTO("Monaco GP");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenUserB);
        HttpEntity<CompetitionRequestDTO> requestEntity = new HttpEntity<>(newCompetitionRequest, headers);

        ResponseEntity<String> response = testRestTemplate.exchange(
                competitionApiUri,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String registerUserAndGetToken(String email) {
        RegisterRequestDTO bettor = new RegisterRequestDTO("Test User", email, "Password123!");
        ResponseEntity<LoginResponseDTO> response =
                testRestTemplate.postForEntity("/api/v1/users/register", bettor, LoginResponseDTO.class);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().token();
    }

    private AuthContext registerUserAndCreateSport(String email, String sportName) {
        String token = registerUserAndGetToken(email);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        SportRequestDTO newSportRequest = new SportRequestDTO(sportName);
        HttpEntity<SportRequestDTO> requestEntity = new HttpEntity<>(newSportRequest, headers);

        ResponseEntity<SportResponseDTO> sportResponse = testRestTemplate.exchange(
                SPORTS_API_URI, HttpMethod.POST, requestEntity, SportResponseDTO.class);

        assertThat(sportResponse.getBody()).isNotNull();
        Long sportId = sportResponse.getBody().id();
        return new AuthContext(token, sportId);
    }

    private Long createSimpleCompetition(AuthContext authContext, String competitionName){
        CompetitionRequestDTO request = new CompetitionRequestDTO(competitionName);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authContext.token);
        HttpEntity<CompetitionRequestDTO> httpEntity = new HttpEntity<>(request, headers);

        ResponseEntity<CompetitionResponseDTO> response = testRestTemplate
                .exchange(SPORTS_API_URI + "/" + authContext.sportId + "/competitions", HttpMethod.POST, httpEntity, CompetitionResponseDTO.class);

        assertThat(response.getBody()).isNotNull();
        return response.getBody().id();
    }

    private record AuthContext(String token, Long sportId) {}
}