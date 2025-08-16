package io.github.joaomnz.bettracker.integration;

import io.github.joaomnz.bettracker.dto.auth.LoginResponseDTO;
import io.github.joaomnz.bettracker.dto.auth.RegisterRequestDTO;
import io.github.joaomnz.bettracker.dto.bookmaker.BookmakerRequestDTO;
import io.github.joaomnz.bettracker.dto.bookmaker.BookmakerResponseDTO;
import io.github.joaomnz.bettracker.dto.transaction.TransactionRequestDTO;
import io.github.joaomnz.bettracker.dto.transaction.TransactionResponseDTO;
import io.github.joaomnz.bettracker.model.enums.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TransactionControllerIT {

    @Autowired
    private TestRestTemplate testRestTemplate;

    private final String BOOKMAKERS_API_URI = "/api/v1/bookmakers";

    @Test
    @DisplayName("Should create a transaction when authenticated and bookmaker belongs to the user")
    void shouldCreateTransactionWhenAuthenticatedAndBookmakerIsValid() {
        AuthContext authContext = registerUserAndCreateBookmaker("user1@email.com", "Bet365");
        String transactionApiUri = BOOKMAKERS_API_URI + "/" + authContext.bookmakerId() + "/transactions";

        TransactionRequestDTO newTransactionRequest = new TransactionRequestDTO(new BigDecimal("100.00"), TransactionType.DEPOSIT);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authContext.token());
        HttpEntity<TransactionRequestDTO> requestEntity = new HttpEntity<>(newTransactionRequest, headers);

        ResponseEntity<TransactionResponseDTO> response = testRestTemplate.exchange(
                transactionApiUri,
                HttpMethod.POST,
                requestEntity,
                TransactionResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().amount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(response.getBody().type()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    @DisplayName("Should return 403 Forbidden when trying to create a transaction without authentication")
    void shouldReturnForbiddenWhenNotAuthenticated() {
        AuthContext authContext = registerUserAndCreateBookmaker("user2@email.com", "Betano");
        String transactionApiUri = BOOKMAKERS_API_URI + "/" + authContext.bookmakerId() + "/transactions";

        TransactionRequestDTO newTransactionRequest = new TransactionRequestDTO(new BigDecimal("50.00"), TransactionType.DEPOSIT);
        HttpEntity<TransactionRequestDTO> requestEntity = new HttpEntity<>(newTransactionRequest);

        ResponseEntity<String> response = testRestTemplate.exchange(
                transactionApiUri,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to create a transaction for a bookmaker that belongs to another user")
    void shouldReturnNotFoundWhenBookmakerBelongsToAnotherUser() {
        AuthContext userA = registerUserAndCreateBookmaker("userA@email.com", "Pinnacle");

        String tokenUserB = registerUserAndGetToken("userB@email.com");

        String transactionApiUri = BOOKMAKERS_API_URI + "/" + userA.bookmakerId() + "/transactions";
        TransactionRequestDTO newTransactionRequest = new TransactionRequestDTO(new BigDecimal("200.00"), TransactionType.DEPOSIT);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenUserB);
        HttpEntity<TransactionRequestDTO> requestEntity = new HttpEntity<>(newTransactionRequest, headers);

        ResponseEntity<String> response = testRestTemplate.exchange(
                transactionApiUri,
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

    private AuthContext registerUserAndCreateBookmaker(String email, String bookmakerName) {
        String token = registerUserAndGetToken(email);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        BookmakerRequestDTO newBookmakerRequest = new BookmakerRequestDTO(bookmakerName);
        HttpEntity<BookmakerRequestDTO> requestEntity = new HttpEntity<>(newBookmakerRequest, headers);

        ResponseEntity<BookmakerResponseDTO> bookmakerResponse = testRestTemplate.exchange(
                BOOKMAKERS_API_URI, HttpMethod.POST, requestEntity, BookmakerResponseDTO.class);
        assertThat(bookmakerResponse.getBody()).isNotNull();
        Long bookmakerId = bookmakerResponse.getBody().id();
        return new AuthContext(token, bookmakerId);
    }

    private record AuthContext(String token, Long bookmakerId) {}
}