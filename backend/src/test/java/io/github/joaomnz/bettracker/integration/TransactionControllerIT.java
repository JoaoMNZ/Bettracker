package io.github.joaomnz.bettracker.integration;

import io.github.joaomnz.bettracker.dto.auth.LoginResponseDTO;
import io.github.joaomnz.bettracker.dto.auth.RegisterRequestDTO;
import io.github.joaomnz.bettracker.dto.bookmaker.BookmakerRequestDTO;
import io.github.joaomnz.bettracker.dto.bookmaker.BookmakerResponseDTO;
import io.github.joaomnz.bettracker.dto.shared.ErrorResponseDTO;
import io.github.joaomnz.bettracker.dto.shared.PageResponseDTO;
import io.github.joaomnz.bettracker.dto.transaction.TransactionRequestDTO;
import io.github.joaomnz.bettracker.dto.transaction.TransactionResponseDTO;
import io.github.joaomnz.bettracker.dto.transaction.UpdateTransactionRequestDTO;
import io.github.joaomnz.bettracker.model.enums.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
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
    @DisplayName("Should return a single transaction when bettor is the owner")
    void findByIdWhenBettorIsOwner() {
        AuthContext authContext = registerUserAndCreateBookmaker("user.get.one@email.com", "Bet365");
        Long transactionId = createSimpleTransaction(authContext, new BigDecimal("100.50"));
        String transactionApiUri = BOOKMAKERS_API_URI + "/" + authContext.bookmakerId() + "/transactions/" + transactionId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authContext.token());
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<TransactionResponseDTO> response = testRestTemplate.exchange(
                transactionApiUri,
                HttpMethod.GET,
                httpEntity,
                TransactionResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(transactionId);
        assertThat(response.getBody().amount()).isEqualTo(new BigDecimal("100.50"));
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to get a transaction from another bettor's bookmaker")
    void findByIdWhenBettorIsNotOwner() {
        AuthContext userA = registerUserAndCreateBookmaker("userA.get@email.com", "Bookmaker A");
        Long transactionIdUserA = createSimpleTransaction(userA, new BigDecimal("200.00"));

        String tokenUserB = registerUserAndGetToken("userB.get@email.com");
        String transactionApiUri = BOOKMAKERS_API_URI + "/" + userA.bookmakerId() + "/transactions/" + transactionIdUserA;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenUserB);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponseDTO> response = testRestTemplate.exchange(
                transactionApiUri,
                HttpMethod.GET,
                httpEntity,
                ErrorResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should return a paginated list of transactions for a given bookmaker")
    void findAllWhenAuthenticated() {
        AuthContext authContext = registerUserAndCreateBookmaker("user.get.all@email.com", "Betano");
        createSimpleTransaction(authContext, new BigDecimal("10.00"));
        createSimpleTransaction(authContext, new BigDecimal("20.00"));
        String transactionApiUri = BOOKMAKERS_API_URI + "/" + authContext.bookmakerId() + "/transactions";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authContext.token());
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ParameterizedTypeReference<PageResponseDTO<TransactionResponseDTO>> responseType = new ParameterizedTypeReference<>() {};

        ResponseEntity<PageResponseDTO<TransactionResponseDTO>> response = testRestTemplate.exchange(
                transactionApiUri,
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
    @DisplayName("Should update a transaction when bettor is the owner of the parent bookmaker")
    void updateWhenBettorIsOwner() {
        AuthContext authContext = registerUserAndCreateBookmaker("user.to.update@email.com", "Bet365");
        Long transactionId = createSimpleTransaction(authContext, new BigDecimal("100.00"));
        String transactionApiUri = BOOKMAKERS_API_URI + "/" + authContext.bookmakerId() + "/transactions/" + transactionId;

        UpdateTransactionRequestDTO updateRequest = new UpdateTransactionRequestDTO(
                new BigDecimal("150.50"), // Novo valor
                TransactionType.WITHDRAWAL    // Novo tipo
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authContext.token());
        HttpEntity<UpdateTransactionRequestDTO> httpEntity = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<TransactionResponseDTO> response = testRestTemplate.exchange(
                transactionApiUri,
                HttpMethod.PATCH,
                httpEntity,
                TransactionResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(transactionId);
        assertThat(response.getBody().amount()).isEqualTo(new BigDecimal("150.50"));
        assertThat(response.getBody().type()).isEqualTo(TransactionType.WITHDRAWAL);
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to update a transaction from another bettor's bookmaker")
    void updateWhenBettorIsNotOwner() {
        AuthContext userA = registerUserAndCreateBookmaker("bettorA.update@email.com", "Bookmaker A");
        Long transactionIdUserA = createSimpleTransaction(userA, new BigDecimal("200.00"));

        String tokenUserB = registerUserAndGetToken("bettorB.update@email.com");

        String transactionApiUri = BOOKMAKERS_API_URI + "/" + userA.bookmakerId() + "/transactions/" + transactionIdUserA;
        UpdateTransactionRequestDTO updateRequest = new UpdateTransactionRequestDTO(new BigDecimal("999.00"), null);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenUserB);
        HttpEntity<UpdateTransactionRequestDTO> httpEntity = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<ErrorResponseDTO> response = testRestTemplate.exchange(
                transactionApiUri,
                HttpMethod.PATCH,
                httpEntity,
                ErrorResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

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

    @Test
    @DisplayName("Should delete a transaction when bettor is the owner of the parent bookmaker")
    void deleteWhenBettorIsOwner() {
        AuthContext authContext = registerUserAndCreateBookmaker("user.to.delete@email.com", "Bet365");
        Long transactionId = createSimpleTransaction(authContext, new BigDecimal("100.00"));
        String transactionApiUri = BOOKMAKERS_API_URI + "/" + authContext.bookmakerId() + "/transactions/" + transactionId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authContext.token());
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<Void> response = testRestTemplate.exchange(
                transactionApiUri,
                HttpMethod.DELETE,
                httpEntity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Should return 404 Not Found when trying to delete a transaction from another bettor's bookmaker")
    void deleteWhenBettorIsNotOwner() {
        AuthContext userA = registerUserAndCreateBookmaker("bettorA.delete@email.com", "Bookmaker A");
        Long transactionIdUserA = createSimpleTransaction(userA, new BigDecimal("50.00"));

        String tokenUserB = registerUserAndGetToken("bettorB.delete@email.com");

        String transactionApiUri = BOOKMAKERS_API_URI + "/" + userA.bookmakerId() + "/transactions/" + transactionIdUserA;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenUserB);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponseDTO> response = testRestTemplate.exchange(
                transactionApiUri,
                HttpMethod.DELETE,
                httpEntity,
                ErrorResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String registerUserAndGetToken(String email) {
        RegisterRequestDTO bettor = new RegisterRequestDTO("Test User", email, "Password123!");
        ResponseEntity<LoginResponseDTO> response =
                testRestTemplate.postForEntity( "/api/v1/users/register", bettor, LoginResponseDTO.class);
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

    private Long createSimpleTransaction(AuthContext authContext, BigDecimal amount) {
        TransactionRequestDTO request = new TransactionRequestDTO(amount, TransactionType.DEPOSIT);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authContext.token());
        HttpEntity<TransactionRequestDTO> httpEntity = new HttpEntity<>(request, headers);

        ResponseEntity<TransactionResponseDTO> response = testRestTemplate.exchange(
                BOOKMAKERS_API_URI + "/" + authContext.bookmakerId() + "/transactions",
                HttpMethod.POST,
                httpEntity,
                TransactionResponseDTO.class
        );
        assertThat(response.getBody()).isNotNull();
        return response.getBody().id();
    }

    private record AuthContext(String token, Long bookmakerId) {}
}