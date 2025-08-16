package io.github.joaomnz.bettracker.integration;

import io.github.joaomnz.bettracker.dto.auth.LoginResponseDTO;
import io.github.joaomnz.bettracker.dto.auth.RegisterRequestDTO;
import io.github.joaomnz.bettracker.dto.tipster.TipsterRequestDTO;
import io.github.joaomnz.bettracker.dto.tipster.TipsterResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
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
    @DisplayName("Should create a tipster when authenticated and data is valid")
    void shouldCreateTipsterWhenAuthenticated() {
        String token = registerBettorAndGetToken();
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

    private String registerBettorAndGetToken() {
        RegisterRequestDTO bettor = new RegisterRequestDTO("João", "joao.menezes21@Outlook.com", "Password123!");
        ResponseEntity<LoginResponseDTO> response =
                testRestTemplate.postForEntity("/api/v1/users/register", bettor, LoginResponseDTO.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().token();
    }
}