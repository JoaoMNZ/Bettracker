package io.github.joaomnz.bettracker.integration;

import io.github.joaomnz.bettracker.dto.auth.LoginResponseDTO;
import io.github.joaomnz.bettracker.dto.auth.RegisterRequestDTO;
import io.github.joaomnz.bettracker.dto.bookmaker.BookmakerRequestDTO;
import io.github.joaomnz.bettracker.dto.bookmaker.BookmakerResponseDTO;
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
public class BookmakerControllerIT {
    @Autowired
    private TestRestTemplate testRestTemplate;

    private final String BOOKMAKER_API_URI = "/api/v1/bookmakers";

    @Test
    @DisplayName("Should create a bookmaker when authenticated and data is valid")
    void shouldCreateBookmakerWhenAuthenticated() {
        BookmakerRequestDTO newBookmaker = new BookmakerRequestDTO("Bet365");
        String token = registerBettorAndGetToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<BookmakerRequestDTO> httpEntity = new HttpEntity<>(newBookmaker, headers);

        ResponseEntity<BookmakerResponseDTO> response =
                testRestTemplate.exchange(BOOKMAKER_API_URI, HttpMethod.POST, httpEntity, BookmakerResponseDTO.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Bet365");
    }

    @Test
    @DisplayName("Should return 403 Forbidden when trying to create a bookmaker without authentication")
    void shouldReturnForbiddenWhenNotAuthenticated() {
        BookmakerRequestDTO newBookmaker = new BookmakerRequestDTO("Bet365");
        HttpEntity<BookmakerRequestDTO> httpEntity = new HttpEntity<>(newBookmaker);
        ResponseEntity<Void> response = testRestTemplate.exchange(BOOKMAKER_API_URI, HttpMethod.POST, httpEntity, Void.class);
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
