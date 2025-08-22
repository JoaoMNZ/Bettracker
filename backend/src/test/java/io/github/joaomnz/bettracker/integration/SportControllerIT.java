package io.github.joaomnz.bettracker.integration;

import io.github.joaomnz.bettracker.dto.auth.LoginResponseDTO;
import io.github.joaomnz.bettracker.dto.auth.RegisterRequestDTO;
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
public class SportControllerIT {
    @Autowired
    private TestRestTemplate testRestTemplate;

    private final String SPORT_API_URI = "/api/v1/sports";

    @Test
    @DisplayName("Should return a single sport when bettor is the owner")
    void findByIdWhenBettorIsOwner() {
        String token = registerBettorAndGetToken("user.get.one@email.com");
        Long sportId = createSportAndGetId(token, "Football");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<SportResponseDTO> response = testRestTemplate.exchange(
                SPORT_API_URI + "/" + sportId,
                HttpMethod.GET,
                httpEntity,
                SportResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(sportId);
        assertThat(response.getBody().name()).isEqualTo("Football");
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to get a sport from another bettor")
    void findByIdWhenBettorIsNotOwner() {
        String tokenBettorA = registerBettorAndGetToken("bettorA.get@email.com");
        Long sportIdBettorA = createSportAndGetId(tokenBettorA, "Sport of A");
        String tokenBettorB = registerBettorAndGetToken("bettorB.get@email.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenBettorB);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponseDTO> response = testRestTemplate.exchange(
                SPORT_API_URI + "/" + sportIdBettorA,
                HttpMethod.GET,
                httpEntity,
                ErrorResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should return a paginated list of sports for the authenticated bettor")
    void findAllWhenAuthenticated() {
        String token = registerBettorAndGetToken("user.get.all@email.com");
        createSportAndGetId(token, "Basketball");
        createSportAndGetId(token, "Tennis");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
        ParameterizedTypeReference<PageResponseDTO<SportResponseDTO>> responseType = new ParameterizedTypeReference<>() {};

        ResponseEntity<PageResponseDTO<SportResponseDTO>> response = testRestTemplate.exchange(
                SPORT_API_URI,
                HttpMethod.GET,
                httpEntity,
                responseType
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(2);
    }

    @Test
    @DisplayName("Should return an empty list when authenticated bettor has no sports")
    void findAllWhenBettorHasNoSports() {
        String token = registerBettorAndGetToken("user.no.sports@email.com");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
        ParameterizedTypeReference<PageResponseDTO<SportResponseDTO>> responseType = new ParameterizedTypeReference<>() {};

        ResponseEntity<PageResponseDTO<SportResponseDTO>> response = testRestTemplate.exchange(
                SPORT_API_URI,
                HttpMethod.GET,
                httpEntity,
                responseType
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).isEmpty();
    }

    @Test
    @DisplayName("Should create a sport when authenticated and data is valid")
    void shouldCreateSportWhenAuthenticated() {
        SportRequestDTO newSport = new SportRequestDTO("Football");
        String token = registerBettorAndGetToken("betor.to.create@email.com");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<SportRequestDTO> httpEntity = new HttpEntity<>(newSport, headers);

        ResponseEntity<SportResponseDTO> response =
                testRestTemplate.exchange(SPORT_API_URI, HttpMethod.POST, httpEntity, SportResponseDTO.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Football");
    }

    @Test
    @DisplayName("Should return 403 Forbidden when trying to create a sport without authentication")
    void shouldReturnForbiddenWhenNotAuthenticated() {
        SportRequestDTO newSport = new SportRequestDTO("Football");
        HttpEntity<SportRequestDTO> httpEntity = new HttpEntity<>(newSport);
        ResponseEntity<Void> response = testRestTemplate.exchange(SPORT_API_URI, HttpMethod.POST, httpEntity, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should update a sport when bettor is the owner")
    void updateWhenBettorIsOwner() {
        String token = registerBettorAndGetToken("user.to.update@email.com");
        Long sportId = createSportAndGetId(token, "Old Name");

        SportRequestDTO updateRequest = new SportRequestDTO("New Name");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<SportRequestDTO> httpEntity = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<SportResponseDTO> response = testRestTemplate.exchange(
                SPORT_API_URI + "/" + sportId,
                HttpMethod.PUT,
                httpEntity,
                SportResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(sportId);
        assertThat(response.getBody().name()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to update a sport from another bettor")
    void updateWhenBettorIsNotOwner() {
        String tokenBettorA = registerBettorAndGetToken("bettorA.update@email.com");
        Long sportIdBettorA = createSportAndGetId(tokenBettorA, "Sport of A");

        String tokenBettorB = registerBettorAndGetToken("bettorB.update@email.com");

        SportRequestDTO updateRequest = new SportRequestDTO("Attempt to change name");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenBettorB);
        HttpEntity<SportRequestDTO> httpEntity = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<ErrorResponseDTO> response = testRestTemplate.exchange(
                SPORT_API_URI + "/" + sportIdBettorA,
                HttpMethod.PUT,
                httpEntity,
                ErrorResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should delete a sport when bettor is the owner")
    void deleteWhenBettorIsOwner() {
        String token = registerBettorAndGetToken("user.to.delete@email.com");
        Long sportId = createSportAndGetId(token, "Sport to Delete");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<Void> response = testRestTemplate.exchange(
                SPORT_API_URI + "/" + sportId,
                HttpMethod.DELETE,
                httpEntity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to delete a sport from another bettor")
    void deleteWhenBettorIsNotOwner() {
        String tokenBettorA = registerBettorAndGetToken("bettorA.delete@email.com");
        Long sportIdBettorA = createSportAndGetId(tokenBettorA, "Sport of A");

        String tokenBettorB = registerBettorAndGetToken("bettorB.delete@email.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenBettorB);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponseDTO> response = testRestTemplate.exchange(
                SPORT_API_URI + "/" + sportIdBettorA,
                HttpMethod.DELETE,
                httpEntity,
                ErrorResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String registerBettorAndGetToken(String email) {
        RegisterRequestDTO bettor = new RegisterRequestDTO("João", email, "Password123!");

        ResponseEntity<LoginResponseDTO> response =
                testRestTemplate.postForEntity("/api/v1/users/register", bettor, LoginResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().token();
    }

    private Long createSportAndGetId(String token, String name) {
        SportRequestDTO request = new SportRequestDTO(name);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<SportRequestDTO> httpEntity = new HttpEntity<>(request, headers);

        ResponseEntity<SportResponseDTO> response =
                testRestTemplate.exchange(SPORT_API_URI, HttpMethod.POST, httpEntity, SportResponseDTO.class);

        assertThat(response.getBody()).isNotNull();
        return response.getBody().id();
    }
}
