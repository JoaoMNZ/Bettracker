package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.IntegrationTest;
import io.github.joaomnz.bettracker.dto.DeactivateAccountRequest;
import io.github.joaomnz.bettracker.dto.UpdateProfileRequest;
import io.github.joaomnz.bettracker.factory.UserFactory;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.repository.UserRepository;
import io.github.joaomnz.bettracker.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserControllerTest extends IntegrationTest {
    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailService emailService;

    private static final String BASE_URL = "/api/v1/users";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should return 200 OK and the user profile information when authenticated.")
    void shouldReturnUserProfileSuccessfully() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());

        performAuthenticatedJsonRequest(get(BASE_URL + "/me"), savedUser, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.name").value(savedUser.getName()))
                .andExpect(jsonPath("$.email").value(savedUser.getEmail()))
                .andExpect(jsonPath("$.unitValue").value(savedUser.getUnitValue().doubleValue()))
                .andExpect(jsonPath("$.userType").value(savedUser.getUserType().name()))
                .andExpect(jsonPath("$.verified").value(savedUser.isVerified()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").isEmpty());
    }

    @Test
    @DisplayName("Should update both name and unit value and return 200 OK.")
    void shouldUpdateBothFieldsSuccessfully() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());
        String newName = "New Nickname";
        BigDecimal newUnit = new BigDecimal("50.0000");

        UpdateProfileRequest request = new UpdateProfileRequest(newName, newUnit);

        performAuthenticatedJsonRequest(patch(BASE_URL + "/me"), savedUser, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.unitValue").value(newUnit.doubleValue()))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.getName()).isEqualTo(newName);
        assertThat(updatedUser.getUnitValue()).isEqualByComparingTo(newUnit);
    }

    @Test
    @DisplayName("Should update only the name and return 200 OK.")
    void shouldUpdateOnlyNameSuccessfully() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());
        String newName = "Only Name Change";
        BigDecimal originalUnit = savedUser.getUnitValue();

        UpdateProfileRequest request = new UpdateProfileRequest(newName, null);

        performAuthenticatedJsonRequest(patch(BASE_URL + "/me"), savedUser, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.unitValue").value(originalUnit.doubleValue()));
    }

    @Test
    @DisplayName("Should update only the unit value and return 200 OK.")
    void shouldUpdateOnlyUnitValueSuccessfully() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());
        String originalName = savedUser.getName();
        BigDecimal newUnit = new BigDecimal("100.0000");

        UpdateProfileRequest request = new UpdateProfileRequest(null, newUnit);

        performAuthenticatedJsonRequest(patch(BASE_URL + "/me"), savedUser, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(originalName))
                .andExpect(jsonPath("$.unitValue").value(newUnit.doubleValue()));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when both fields are null.")
    void shouldReturn400WhenNoFieldsProvided() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());

        UpdateProfileRequest request = new UpdateProfileRequest(null, null);

        performAuthenticatedJsonRequest(patch(BASE_URL + "/me"), savedUser, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.validUpdate").exists());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when name is only whitespace.")
    void shouldReturn400WhenNameIsBlank() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());

        UpdateProfileRequest request = new UpdateProfileRequest("    ", null);

        performAuthenticatedJsonRequest(patch(BASE_URL + "/me"), savedUser, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when unit value is negative or zero.")
    void shouldReturn400WhenUnitValueIsInvalid() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());

        UpdateProfileRequest request = new UpdateProfileRequest(null, BigDecimal.ZERO);

        performAuthenticatedJsonRequest(patch(BASE_URL + "/me"), savedUser, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.unitValue").exists());
    }

    @Test
    @DisplayName("Should deactivate account, trigger email, and return 204 No Content when provided with correct password.")
    void shouldDeactivateAccountSuccessfully() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());
        DeactivateAccountRequest request = new DeactivateAccountRequest(UserFactory.DEFAULT_PASSWORD);

        performAuthenticatedJsonRequest(delete(BASE_URL + "/me"), savedUser, request)
                .andExpect(status().isNoContent());

        User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.isActive()).isFalse();

        verify(emailService, times(1)).sendDeactivationEmail(savedUser.getEmail(), savedUser.getName());
    }

    @Test
    @DisplayName("Should return 400 Bad Request and not deactivate when password is wrong.")
    void shouldReturn400WhenPasswordIsWrong() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());
        DeactivateAccountRequest request = new DeactivateAccountRequest("incorrect-password");

        performAuthenticatedJsonRequest(delete(BASE_URL + "/me"), savedUser, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Incorrect password."));

        User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.isActive()).isTrue();

        verify(emailService, never()).sendDeactivationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when attempting to deactivate an already deactivated account.")
    void shouldReturn400WhenAccountIsAlreadyDeactivated() throws Exception {
        User user = UserFactory.createUser();
        user.setActive(false);
        User savedUser = userRepository.save(user);

        DeactivateAccountRequest request = new DeactivateAccountRequest(UserFactory.DEFAULT_PASSWORD);

        performAuthenticatedJsonRequest(delete(BASE_URL + "/me"), savedUser, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Account is already deactivated."));

        verify(emailService, never()).sendDeactivationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when password is blank.")
    void shouldReturn400WhenPasswordIsBlank() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());
        DeactivateAccountRequest request = new DeactivateAccountRequest("");

        performAuthenticatedJsonRequest(delete(BASE_URL + "/me"), savedUser, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password").exists());

        verify(emailService, never()).sendDeactivationEmail(anyString(), anyString());
    }
}