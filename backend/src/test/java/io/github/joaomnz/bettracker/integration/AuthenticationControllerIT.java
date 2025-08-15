package io.github.joaomnz.bettracker.integration;

import io.github.joaomnz.bettracker.dto.auth.LoginRequestDTO;
import io.github.joaomnz.bettracker.dto.auth.LoginResponseDTO;
import io.github.joaomnz.bettracker.dto.auth.RegisterRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthenticationControllerIT {
    @Autowired
    private TestRestTemplate testRestTemplate;

    private final String AUTH_API_URI = "/api/v1/auth";
    private final String USERS_API_URI = "/api/v1/users";

    private RegisterRequestDTO defaultUser;

    @BeforeEach
    void setUp() {
        defaultUser = new RegisterRequestDTO("Test User", "test@email.com", "Password123!");
        testRestTemplate.postForEntity(USERS_API_URI + "/register", defaultUser, LoginResponseDTO.class);
    }

    @Test
    @DisplayName("Should return 200 OK and JWT when logging in with valid credentials")
    void shouldReturnOkAndTokenWhenLoginWithValidCredentials() {
        LoginRequestDTO loginRequest = new LoginRequestDTO(defaultUser.email(), defaultUser.password());
        ResponseEntity<LoginResponseDTO> response = testRestTemplate.postForEntity(
                AUTH_API_URI + "/login",
                loginRequest,
                LoginResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(response.getBody().email()).isEqualTo(defaultUser.email());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when logging in with wrong password")
    void shouldReturnForbiddenWhenLoginWithWrongPassword() {
        LoginRequestDTO loginRequest = new LoginRequestDTO(defaultUser.email(), "wrong-password");
        ResponseEntity<String> response = testRestTemplate.postForEntity(
                AUTH_API_URI + "/login",
                loginRequest,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should return 403 Forbidden when logging in with a non-existent email")
    void shouldReturnForbiddenWhenLoginWithNonExistentUser() {
        LoginRequestDTO loginRequest = new LoginRequestDTO("nonexistent@email.com", "any-password");

        ResponseEntity<String> response = testRestTemplate.postForEntity(
                AUTH_API_URI + "/login",
                loginRequest,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}