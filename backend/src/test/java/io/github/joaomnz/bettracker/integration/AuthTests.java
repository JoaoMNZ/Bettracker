package io.github.joaomnz.bettracker.integration;

import io.github.joaomnz.bettracker.dto.auth.LoginResponseDTO;
import io.github.joaomnz.bettracker.dto.auth.RegisterRequestDTO;
import io.github.joaomnz.bettracker.dto.bettor.BettorResponseDTO;
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
public class AuthTests {
    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    @DisplayName("Should return user data when providing a valid JWT token")
    void shouldReturnUserData_whenValidTokenIsProvided() {
        RegisterRequestDTO registerDTO = new RegisterRequestDTO(
                "Authenticated User",
                "auth.user@email.com",
                "password123"
        );
        ResponseEntity<LoginResponseDTO> registerResponse = testRestTemplate.postForEntity(
                "/api/v1/auth/register",
                registerDTO,
                LoginResponseDTO.class
        );
        String token = registerResponse.getBody().token();
        assertThat(token).isNotBlank();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<BettorResponseDTO> userResponse = testRestTemplate.exchange(
                "/api/v1/users/me",
                HttpMethod.GET,
                requestEntity,
                BettorResponseDTO.class
        );
        assertThat(userResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userResponse.getBody()).isNotNull();
        assertThat(userResponse.getBody().name()).isEqualTo("Authenticated User");
        assertThat(userResponse.getBody().email()).isEqualTo("auth.user@email.com");
    }

    @Test
    @DisplayName("Should return Forbidden when trying to access a protected endpoint with an invalid token")
    void shouldReturnForbidden_whenTokenIsInvalid() {
        String invalidToken = "Invalid token.";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(invalidToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = testRestTemplate.exchange(
                "/api/v1/users/me",
                HttpMethod.GET,
                requestEntity,
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
