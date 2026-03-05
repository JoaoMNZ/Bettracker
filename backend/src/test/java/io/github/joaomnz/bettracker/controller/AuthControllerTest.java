package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.dto.SignInRequest;
import io.github.joaomnz.bettracker.dto.SignUpRequest;
import io.github.joaomnz.bettracker.enums.UserType;
import io.github.joaomnz.bettracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/auth";

    @BeforeEach
    void setUp(){
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should sign up a new user and return 201 Created with a JWT and summary user information when provided with valid data.")
    void shouldSignUpUserSuccessfully() throws Exception {
        SignUpRequest signUpRequest = defaultSignUpRequest();
        performJsonRequest(post(BASE_URL + "/signup"), signUpRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.name").value(signUpRequest.name()))
                .andExpect(jsonPath("$.user.email").value(signUpRequest.email()))
                .andExpect(jsonPath("$.user.unitValue").value(signUpRequest.unitValue().doubleValue()))
                .andExpect(jsonPath("$.user.userType").value(UserType.FREE.name()))
                .andExpect(jsonPath("$.user.verified").value(Boolean.FALSE));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return 400 Bad Request and validation errors when data is invalid.")
    void shouldReturn400WhenValidationFails() throws Exception {
        SignUpRequest signUpRequest = new SignUpRequest(
                "",
                "invalid-email",
                "Pass123!",
                BigDecimal.TEN
        );
        performJsonRequest(post(BASE_URL + "/signup"), signUpRequest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists());

        assertThat(userRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return 409 Conflict when attempting to sign up an existing email.")
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        SignUpRequest firstUser = defaultSignUpRequest();
        performJsonRequest(post(BASE_URL + "/signup"), firstUser);

        SignUpRequest secondUser = new SignUpRequest(
                "test 2",
                firstUser.email(),
                "Pass123!",
                BigDecimal.TEN
        );
        performJsonRequest(post(BASE_URL + "/signup"), secondUser)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return 200 OK with a JWT and summary user information when signing in with valid credentials.")
    void shouldSignInSuccessfully() throws Exception{
        SignUpRequest signUpRequest = defaultSignUpRequest();
        performJsonRequest(post(BASE_URL + "/signup"), signUpRequest);

        SignInRequest signInRequest = new SignInRequest(
                signUpRequest.email(),
                signUpRequest.password()
        );
        performJsonRequest(post(BASE_URL + "/signin"), signInRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.name").value(signUpRequest.name()))
                .andExpect(jsonPath("$.user.email").value(signUpRequest.email()))
                .andExpect(jsonPath("$.user.unitValue").value(signUpRequest.unitValue().doubleValue()))
                .andExpect(jsonPath("$.user.userType").value(UserType.FREE.name()))
                .andExpect(jsonPath("$.user.verified").value(Boolean.FALSE));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when signing in with incorrect password")
    void shouldReturn401WhenPasswordIsIncorrect() throws Exception{
        SignUpRequest signUpRequest = defaultSignUpRequest();
        performJsonRequest(post(BASE_URL + "/signup"), signUpRequest);

        SignInRequest signInRequest = new SignInRequest(
                signUpRequest.email(),
                "incorrect-password"
        );
        performJsonRequest(post(BASE_URL + "/signin"), signInRequest)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private SignUpRequest defaultSignUpRequest() {
        return new SignUpRequest(
                "test",
                "test@hotmail.com",
                "Pass123!",
                BigDecimal.TEN
        );
    }

    private ResultActions performJsonRequest(MockHttpServletRequestBuilder builder, Object body) throws Exception {
        return mockMvc.perform(
                builder
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
        );
    }
}