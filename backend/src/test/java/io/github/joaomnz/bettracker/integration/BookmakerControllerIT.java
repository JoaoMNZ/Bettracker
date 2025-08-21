package io.github.joaomnz.bettracker.integration;

import io.github.joaomnz.bettracker.dto.auth.LoginResponseDTO;
import io.github.joaomnz.bettracker.dto.auth.RegisterRequestDTO;
import io.github.joaomnz.bettracker.dto.bookmaker.BookmakerRequestDTO;
import io.github.joaomnz.bettracker.dto.bookmaker.BookmakerResponseDTO;
import io.github.joaomnz.bettracker.dto.shared.ErrorResponseDTO;
import io.github.joaomnz.bettracker.dto.shared.PageResponseDTO;
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
public class BookmakerControllerIT {
    @Autowired
    private TestRestTemplate testRestTemplate;

    private final String BOOKMAKER_API_URI = "/api/v1/bookmakers";

    @Test
    @DisplayName("Should return a single bookmaker when bettor is the owner")
    void findByIdWhenBettorIsOwner() {
        String token = registerBettorAndGetToken("user.get.one@email.com");
        Long bookmakerId = createBookmakerAndGetId(token, "Bet365");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<BookmakerResponseDTO> response = testRestTemplate.exchange(
                BOOKMAKER_API_URI + "/" + bookmakerId,
                HttpMethod.GET,
                httpEntity,
                BookmakerResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(bookmakerId);
        assertThat(response.getBody().name()).isEqualTo("Bet365");
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to get a bookmaker from another bettor")
    void findByIdWhenBettorIsNotOwner() {
        String tokenBettorA = registerBettorAndGetToken("bettorA.get@email.com");
        Long bookmakerIdBettorA = createBookmakerAndGetId(tokenBettorA, "Bookmaker of A");

        String tokenBettorB = registerBettorAndGetToken("bettorB.get@email.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenBettorB);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponseDTO> response = testRestTemplate.exchange(
                BOOKMAKER_API_URI + "/" + bookmakerIdBettorA,
                HttpMethod.GET,
                httpEntity,
                ErrorResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should return a paginated list of bookmakers for the authenticated bettor")
    void findAllWhenAuthenticated() {
        String token = registerBettorAndGetToken("user.get.all@email.com");
        createBookmakerAndGetId(token, "Betano");
        createBookmakerAndGetId(token, "Pinnacle");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ParameterizedTypeReference<PageResponseDTO<BookmakerResponseDTO>> responseType = new ParameterizedTypeReference<>() {};

        ResponseEntity<PageResponseDTO<BookmakerResponseDTO>> response = testRestTemplate.exchange(
                BOOKMAKER_API_URI,
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
    @DisplayName("Should return an empty list when authenticated bettor has no bookmakers")
    void findAllWhenAuthenticatedBettorHasNoBookmakers() {
        String token = registerBettorAndGetToken("user.no.bookmakers@email.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ParameterizedTypeReference<PageResponseDTO<BookmakerResponseDTO>> responseType = new ParameterizedTypeReference<>() {};

        ResponseEntity<PageResponseDTO<BookmakerResponseDTO>> response = testRestTemplate.exchange(
                BOOKMAKER_API_URI,
                HttpMethod.GET,
                httpEntity,
                responseType
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).isEmpty();
    }

    @Test
    @DisplayName("Should create a bookmaker when authenticated and data is valid")
    void shouldCreateBookmakerWhenAuthenticated() {
        BookmakerRequestDTO newBookmaker = new BookmakerRequestDTO("Bet365");
        String token = registerBettorAndGetToken("bettorAcreate@gmail.com");
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

    @Test
    @DisplayName("Should update a bookmaker when bettor is the owner")
    void updateWhenBettorIsOwner() {
        String token = registerBettorAndGetToken("user.to.update@email.com");
        Long bookmakerId = createBookmakerAndGetId(token, "Old Name");

        BookmakerRequestDTO updateRequest = new BookmakerRequestDTO("New Name");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<BookmakerRequestDTO> httpEntity = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<BookmakerResponseDTO> response = testRestTemplate.exchange(
                BOOKMAKER_API_URI + "/" + bookmakerId,
                HttpMethod.PUT,
                httpEntity,
                BookmakerResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(bookmakerId);
        assertThat(response.getBody().name()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to update a bookmaker from another bettor")
    void updateWhenBettorIsNotOwner() {
        String tokenBettorA = registerBettorAndGetToken("bettorA.update@email.com");
        Long bookmakerIdBettorA = createBookmakerAndGetId(tokenBettorA, "Bookmaker of A");

        String tokenBettorB = registerBettorAndGetToken("bettorB.update@email.com");

        BookmakerRequestDTO updateRequest = new BookmakerRequestDTO("Attempt to change name");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenBettorB);
        HttpEntity<BookmakerRequestDTO> httpEntity = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<ErrorResponseDTO> response = testRestTemplate.exchange(
                BOOKMAKER_API_URI + "/" + bookmakerIdBettorA,
                HttpMethod.PUT,
                httpEntity,
                ErrorResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should delete a bookmaker when bettor is the owner")
    void deleteWhenBettorIsOwner() {
        String token = registerBettorAndGetToken("user.to.delete@email.com");
        Long bookmakerId = createBookmakerAndGetId(token, "Bookmaker to Delete");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<Void> response = testRestTemplate.exchange(
                BOOKMAKER_API_URI + "/" + bookmakerId,
                HttpMethod.DELETE,
                httpEntity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to delete a bookmaker from another bettor")
    void deleteWhenBettorIsNotOwner() {
        String tokenBettorA = registerBettorAndGetToken("bettorA.delete@email.com");
        Long bookmakerIdBettorA = createBookmakerAndGetId(tokenBettorA, "Bookmaker of A");

        String tokenBettorB = registerBettorAndGetToken("bettorB.delete@email.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenBettorB);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponseDTO> response = testRestTemplate.exchange(
                BOOKMAKER_API_URI + "/" + bookmakerIdBettorA,
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

    private Long createBookmakerAndGetId(String token, String name) {
        BookmakerRequestDTO request = new BookmakerRequestDTO(name);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<BookmakerRequestDTO> httpEntity = new HttpEntity<>(request, headers);

        ResponseEntity<BookmakerResponseDTO> response =
                testRestTemplate.exchange(BOOKMAKER_API_URI, HttpMethod.POST, httpEntity, BookmakerResponseDTO.class);

        assertThat(response.getBody()).isNotNull();
        return response.getBody().id();
    }
}
