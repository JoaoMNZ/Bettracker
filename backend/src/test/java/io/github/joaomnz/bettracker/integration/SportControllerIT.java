package io.github.joaomnz.bettracker.integration;

import io.github.joaomnz.bettracker.dto.auth.LoginResponseDTO;
import io.github.joaomnz.bettracker.dto.auth.RegisterRequestDTO;
import io.github.joaomnz.bettracker.dto.sport.SportRequestDTO;
import io.github.joaomnz.bettracker.dto.sport.SportResponseDTO;
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
public class SportControllerIT {
    @Autowired
    private TestRestTemplate testRestTemplate;

    private final String SPORT_API_URI = "/api/v1/sports";

    @Test
    @DisplayName("Should create a sport when authenticated and data is valid")
    void shouldCreateSportWhenAuthenticated() {
        SportRequestDTO newSport = new SportRequestDTO("Football");
        String token = registerBettorAndGetToken();
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

    private String registerBettorAndGetToken() {
        RegisterRequestDTO bettor = new RegisterRequestDTO("João", "joao.menezes21@Outlook.com", "Password123!");
        ResponseEntity<LoginResponseDTO> response =
                testRestTemplate.postForEntity("/api/v1/users/register", bettor, LoginResponseDTO.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().token();
    }
}
