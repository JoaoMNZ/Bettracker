package io.github.joaomnz.bettracker.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.joaomnz.bettracker.dto.auth.LoginResponseDTO;
import io.github.joaomnz.bettracker.dto.auth.RegisterRequestDTO;
import io.github.joaomnz.bettracker.dto.bettor.BettorResponseDTO;
import io.github.joaomnz.bettracker.dto.shared.ErrorResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BettorControllerIT {
    @Autowired
    TestRestTemplate testRestTemplate;

    private final String API_URI = "/api/v1/users";

    @Test
    @DisplayName("Should return Created when trying to registering with a valid data")
    void shouldReturnCreatedWhenRegisteringWithValidData(){
        RegisterRequestDTO request = new RegisterRequestDTO("João", "joao.menezes21@Outlook.com", "Admin12!");

        ResponseEntity<LoginResponseDTO> registerResponse =
                testRestTemplate.postForEntity(API_URI + "/register", request, LoginResponseDTO.class);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().token()).isNotNull();
        assertThat(registerResponse.getBody().name()).isEqualTo("João");
    }

    @Test
    @DisplayName("Should return 409 Conflict when trying to register with an existing email")
    void shouldReturnConflictWhenEmailAlreadyExists() {
        RegisterRequestDTO request = new RegisterRequestDTO("João", "joao.menezes21@Outlook.com", "Admin12!");

        testRestTemplate.postForEntity(API_URI + "/register", request, LoginResponseDTO.class);

        ResponseEntity<ErrorResponseDTO> response =
                testRestTemplate.postForEntity(API_URI + "/register", request, ErrorResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Email '" + request.email() + "' is already in use.");
    }

    @Test
    @DisplayName("Should return 400 Bad Request when registering with an invalid email format")
    void shouldReturnBadRequestWhenEmailIsInvalid() {
        RegisterRequestDTO request = new RegisterRequestDTO("João", "joao.21Outlook.com", "Admin12!");

        ResponseEntity<ErrorResponseDTO> response =
                testRestTemplate.postForEntity(API_URI + "/register", request, ErrorResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().validationErrors().get("email")).isEqualTo("Invalid email format.");
    }

    @Test
    @DisplayName("Should return 400 Bad Request when registering with a weak password")
    void shouldReturnBadRequestWhenPasswordIsWeak() {
        RegisterRequestDTO request = new RegisterRequestDTO("João", "joao.menezes21@Outlook.com", "Admin12");

        ResponseEntity<ErrorResponseDTO> response =
                testRestTemplate.postForEntity(API_URI + "/register", request, ErrorResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().validationErrors().get("password"))
                .isEqualTo("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character.");
    }

    @Test
    @DisplayName("Should return 200 OK and bettor data when accessing /me with a valid token")
    void shouldReturnOkAndUserDataWhenTokenIsValid() {
        String token = registerBettorAndGetToken("User Me", "me@email.com", "Password123!");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<BettorResponseDTO> response = testRestTemplate.exchange(
                API_URI + "/me",
                HttpMethod.GET,
                requestEntity,
                BettorResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo("me@email.com");
    }

    @Test
    @DisplayName("Should return 403 Forbidden when accessing /me without a token")
    void shouldReturnForbiddenWhenNoTokenIsProvided() {
        ResponseEntity<String> response = testRestTemplate.exchange(
                API_URI + "/me",
                HttpMethod.GET,
                null,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should return 403 Forbidden when accessing /me with an invalid/fake token")
    void shouldReturnForbiddenWhenTokenIsInvalid() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("this.is.a.fake.token");
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = testRestTemplate.exchange(
                API_URI + "/me",
                HttpMethod.GET,
                requestEntity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private String registerBettorAndGetToken(String name, String email, String password) {
        RegisterRequestDTO registerRequest = new RegisterRequestDTO(name, email, password);
        ResponseEntity<LoginResponseDTO> response = testRestTemplate.postForEntity(
                API_URI + "/register",
                registerRequest,
                LoginResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().token();
    }
}

