/*
package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.IntegrationTest;
import io.github.joaomnz.bettracker.dto.user.*;
import io.github.joaomnz.bettracker.factory.OtpTokenFactory;
import io.github.joaomnz.bettracker.factory.UserFactory;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.repository.OtpTokenRepository;
import io.github.joaomnz.bettracker.repository.UserRepository;
import io.github.joaomnz.bettracker.service.EmailService;
import io.github.joaomnz.bettracker.service.OtpTokenService;
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

    @Autowired
    private OtpTokenRepository otpTokenRepository;

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
    @DisplayName("Should update password, trigger email, and return 204 No Content when old password is correct.")
    void shouldUpdatePasswordSuccessfully() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());
        String newPassword = "NewStrongPassword123!";

        UpdatePasswordRequest request = new UpdatePasswordRequest(UserFactory.DEFAULT_PASSWORD, newPassword);

        performAuthenticatedJsonRequest(put(BASE_URL + "/me/password"), savedUser, request)
                .andExpect(status().isNoContent());

        User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.getPassword()).isNotEqualTo(savedUser.getPassword());

        verify(emailService, times(1)).sendPasswordChangeNotice(savedUser.getEmail(), savedUser.getName());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when current password is incorrect.")
    void shouldReturn400WhenOldPasswordIsWrong() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());

        UpdatePasswordRequest request = new UpdatePasswordRequest("incorrect-password", "new" + UserFactory.DEFAULT_PASSWORD);

        performAuthenticatedJsonRequest(put(BASE_URL + "/me/password"), savedUser, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Incorrect current password."));

        verify(emailService, never()).sendPasswordChangeNotice(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when new password is the same as the old password.")
    void shouldReturn400WhenNewPasswordIsSameAsOld() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());

        UpdatePasswordRequest request = new UpdatePasswordRequest(UserFactory.DEFAULT_PASSWORD, UserFactory.DEFAULT_PASSWORD);

        performAuthenticatedJsonRequest(put(BASE_URL + "/me/password"), savedUser, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password cannot be the same as your current password."));

        verify(emailService, never()).sendPasswordChangeNotice(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when new password does not meet complexity requirements.")
    void shouldReturn400WhenNewPasswordIsWeak() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());

        UpdatePasswordRequest request = new UpdatePasswordRequest(UserFactory.DEFAULT_PASSWORD, "weakpassword");

        performAuthenticatedJsonRequest(put(BASE_URL + "/me/password"), savedUser, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.newPassword").exists());

        verify(emailService, never()).sendPasswordChangeNotice(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 204 No Content, save pending email, and trigger OTP email when requesting an email change.")
    void shouldRequestEmailChangeSuccessfully() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());
        String newEmail = "new.email@example.com";

        RequestEmailChangeRequest request = new RequestEmailChangeRequest(newEmail, UserFactory.DEFAULT_PASSWORD);

        performAuthenticatedJsonRequest(post(BASE_URL + "/me/email/request-change"), savedUser, request)
                .andExpect(status().isNoContent());

        User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.getPendingEmail()).isEqualTo(newEmail);

        verify(emailService, times(1)).sendEmailChangeVerificationEmail(eq(newEmail), eq(savedUser.getName()), anyString());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when current password is wrong during email change request.")
    void shouldReturn400WhenPasswordIsWrongOnEmailChange() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());
        String newEmail = "new.email@example.com";
        String invalidPassword = "wrong-password";

        RequestEmailChangeRequest request = new RequestEmailChangeRequest(newEmail, invalidPassword);

        performAuthenticatedJsonRequest(post(BASE_URL + "/me/email/request-change"), savedUser, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Incorrect current password."));

        verify(emailService, never()).sendEmailChangeVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when new email is the same as the current email.")
    void shouldReturn400WhenNewEmailIsSameAsCurrent() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());

        RequestEmailChangeRequest request = new RequestEmailChangeRequest(savedUser.getEmail(), UserFactory.DEFAULT_PASSWORD);

        performAuthenticatedJsonRequest(post(BASE_URL + "/me/email/request-change"), savedUser, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The new email must be different from your current email."));

        verify(emailService, never()).sendEmailChangeVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 409 Conflict when attempting to change to an email that is already taken.")
    void shouldReturn409WhenNewEmailIsAlreadyRegistered() throws Exception {
        User firstUser = userRepository.save(UserFactory.createUser());
        User secondUser = userRepository.save(UserFactory.createUser("taken@example.com"));

        RequestEmailChangeRequest request = new RequestEmailChangeRequest(secondUser.getEmail(), UserFactory.DEFAULT_PASSWORD);

        performAuthenticatedJsonRequest(post(BASE_URL + "/me/email/request-change"), firstUser, request)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The email address is already registered."));

        verify(emailService, never()).sendEmailChangeVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 204 No Content, update email, clear pending, set verified, and trigger notice when verifying email change.")
    void shouldVerifyEmailChangeSuccessfully() throws Exception {
        User user = UserFactory.createUser();
        String oldEmail = user.getEmail();
        String pendingEmail = "new.verified@example.com";
        user.setPendingEmail(pendingEmail);
        User savedUser = userRepository.save(user);

        otpTokenRepository.save(OtpTokenFactory.createEmailChange(user));

        VerifyEmailChangeRequest request = new VerifyEmailChangeRequest(OtpTokenFactory.DEFAULT_CODE);

        performAuthenticatedJsonRequest(post(BASE_URL + "/me/email/verify-change"), savedUser, request)
                .andExpect(status().isNoContent());

        User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.getEmail()).isEqualTo(pendingEmail);
        assertThat(updatedUser.getPendingEmail()).isNull();
        assertThat(updatedUser.isVerified()).isTrue();

        verify(emailService, times(1)).sendEmailChangeNotice(eq(oldEmail), eq(savedUser.getName()));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when attempting to verify an email change without a pending request.")
    void shouldReturn400OnVerifyEmailChangeWhenNoPendingEmail() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());

        VerifyEmailChangeRequest request = new VerifyEmailChangeRequest(OtpTokenFactory.DEFAULT_CODE);

        performAuthenticatedJsonRequest(post(BASE_URL + "/me/email/verify-change"), savedUser, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No email change request is currently pending."));

        verify(emailService, never()).sendEmailChangeNotice(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when OTP is invalid during email change verification.")
    void shouldReturn400OnVerifyEmailChangeWhenOtpIsInvalid() throws Exception {
        User user = UserFactory.createUser();
        user.setPendingEmail("new.email@example.com");
        User savedUser = userRepository.save(user);

        otpTokenRepository.save(OtpTokenFactory.createEmailChange(user));

        String invalidOtp = "111111";
        VerifyEmailChangeRequest request = new VerifyEmailChangeRequest(invalidOtp);

        performAuthenticatedJsonRequest(post(BASE_URL + "/me/email/verify-change"), savedUser, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid verification code. You have 2 attempt(s) left."));

        User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();

        assertThat(updatedUser.getPendingEmail()).isNotNull();
        assertThat(updatedUser.getEmail()).isNotEqualTo(user.getPendingEmail());

        verify(emailService, never()).sendEmailChangeNotice(anyString(), anyString());
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
*/