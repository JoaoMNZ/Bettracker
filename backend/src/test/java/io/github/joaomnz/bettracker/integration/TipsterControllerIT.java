package io.github.joaomnz.bettracker.integration;

import io.github.joaomnz.bettracker.dto.auth.LoginResponseDTO;
import io.github.joaomnz.bettracker.dto.auth.RegisterRequestDTO;
import io.github.joaomnz.bettracker.dto.shared.ErrorResponseDTO;
import io.github.joaomnz.bettracker.dto.shared.PageResponseDTO;
import io.github.joaomnz.bettracker.dto.tipster.TipsterRequestDTO;
import io.github.joaomnz.bettracker.dto.tipster.TipsterResponseDTO;
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
public class TipsterControllerIT {

    @Autowired
    private TestRestTemplate testRestTemplate;

    private final String TIPSTER_API_URI = "/api/v1/tipsters";

    @Test
    @DisplayName("Should return a single tipster when bettor is the owner")
    void findByIdWhenBettorIsOwner() {
        String token = registerBettorAndGetToken("user.get.one@email.com");
        Long tipsterId = createTipsterAndGetId(token, "Specific Tipster");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<TipsterResponseDTO> response = testRestTemplate.exchange(
                TIPSTER_API_URI + "/" + tipsterId,
                HttpMethod.GET,
                httpEntity,
                TipsterResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(tipsterId);
        assertThat(response.getBody().name()).isEqualTo("Specific Tipster");
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to get a tipster from another bettor")
    void findByIdWhenBettorIsNotOwner() {
        String tokenBettorA = registerBettorAndGetToken("bettorA.get@email.com");
        Long tipsterIdBettorA = createTipsterAndGetId(tokenBettorA, "Tipster of A");

        String tokenBettorB = registerBettorAndGetToken("bettorB.get@email.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenBettorB);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponseDTO> response = testRestTemplate.exchange(
                TIPSTER_API_URI + "/" + tipsterIdBettorA,
                HttpMethod.GET,
                httpEntity,
                ErrorResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should return a paginated list of tipsters for the authenticated bettor")
    void findAllWhenAuthenticated() {
        String token = registerBettorAndGetToken("user.get.all@email.com");
        createTipsterAndGetId(token, "Tipster 1");
        createTipsterAndGetId(token, "Tipster 2");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ParameterizedTypeReference<PageResponseDTO<TipsterResponseDTO>> responseType = new ParameterizedTypeReference<>() {};

        ResponseEntity<PageResponseDTO<TipsterResponseDTO>> response = testRestTemplate.exchange(
                TIPSTER_API_URI,
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
    @DisplayName("Should return an empty list when authenticated bettor has no tipsters")
    void findAllWhenAuthenticatedBettorHasNoTipsters() {
        String token = registerBettorAndGetToken("user.no.tipsters@email.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ParameterizedTypeReference<PageResponseDTO<TipsterResponseDTO>> responseType = new ParameterizedTypeReference<>() {};

        ResponseEntity<PageResponseDTO<TipsterResponseDTO>> response = testRestTemplate.exchange(
                TIPSTER_API_URI,
                HttpMethod.GET,
                httpEntity,
                responseType
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).isEmpty();
    }

    @Test
    @DisplayName("Should create a tipster when authenticated and data is valid")
    void shouldCreateTipsterWhenAuthenticated() {
        String token = registerBettorAndGetToken("bettorA.create@email.com");
        TipsterRequestDTO newTipsterRequest = new TipsterRequestDTO("Pei!");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<TipsterRequestDTO> requestEntity = new HttpEntity<>(newTipsterRequest, headers);

        ResponseEntity<TipsterResponseDTO> response = testRestTemplate.exchange(
                TIPSTER_API_URI,
                HttpMethod.POST,
                requestEntity,
                TipsterResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Pei!");
    }

    @Test
    @DisplayName("Should return 403 Forbidden when trying to create a tipster without authentication")
    void shouldReturnForbiddenWhenNotAuthenticated() {
        TipsterRequestDTO newTipsterRequest = new TipsterRequestDTO("Pei!");

        HttpEntity<TipsterRequestDTO> requestEntity = new HttpEntity<>(newTipsterRequest);

        ResponseEntity<String> response = testRestTemplate.exchange(
                TIPSTER_API_URI,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private String registerBettorAndGetToken(String email) {
        RegisterRequestDTO bettor = new RegisterRequestDTO("João", email, "Password123!");

        ResponseEntity<LoginResponseDTO> response =
                testRestTemplate.postForEntity("/api/v1/users/register", bettor, LoginResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().token();
    }

    private Long createTipsterAndGetId(String token, String name) {
        TipsterRequestDTO request = new TipsterRequestDTO(name);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<TipsterRequestDTO> httpEntity = new HttpEntity<>(request, headers);

        ResponseEntity<TipsterResponseDTO> response =
                testRestTemplate.exchange(TIPSTER_API_URI, HttpMethod.POST, httpEntity, TipsterResponseDTO.class);

        assertThat(response.getBody()).isNotNull();
        return response.getBody().id();
    }
}