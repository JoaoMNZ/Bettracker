package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.IntegrationTest;
import io.github.joaomnz.bettracker.dto.auth.*;
import io.github.joaomnz.bettracker.dto.user.ForgotPasswordRequest;
import io.github.joaomnz.bettracker.dto.auth.ResetPasswordRequest;
import io.github.joaomnz.bettracker.enums.UserType;
import io.github.joaomnz.bettracker.factory.OtpTokenFactory;
import io.github.joaomnz.bettracker.factory.RefreshTokenFactory;
import io.github.joaomnz.bettracker.factory.UserFactory;
import io.github.joaomnz.bettracker.model.OtpToken;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.repository.OtpTokenRepository;
import io.github.joaomnz.bettracker.repository.RefreshTokenRepository;
import io.github.joaomnz.bettracker.repository.UserRepository;
import io.github.joaomnz.bettracker.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

public class AuthControllerTest extends IntegrationTest {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpTokenRepository otpTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    // Prevents real SMTP network calls and avoids spamming Mailtrap during test execution.
    @MockitoBean
    private EmailService emailService;

    private static final String BASE_URL = "/api/v1/auth";

    @BeforeEach
    void setUp() {
        otpTokenRepository.deleteAll();
        userRepository.deleteAll();
        refreshTokenRepository.deleteAll();
    }

    @Test
    @DisplayName("Should sign up a new user and return 201 Created with a JWT and summary user information when provided with valid data.")
    void shouldSignUpUserSuccessfully() throws Exception {
        SignUpRequest signUpRequest = UserFactory.createSignUpRequest();
        performJsonRequest(post(BASE_URL + "/signup"), signUpRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.name").value(signUpRequest.name()))
                .andExpect(jsonPath("$.user.email").value(signUpRequest.email()))
                .andExpect(jsonPath("$.user.unitValue").value(signUpRequest.unitValue().doubleValue()))
                .andExpect(jsonPath("$.user.userType").value(UserType.FREE.name()))
                .andExpect(jsonPath("$.user.verified").value(Boolean.FALSE));

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.count()).isEqualTo(1);
        verify(emailService, times(1)).sendVerificationEmail(
                eq(signUpRequest.email()),
                eq(signUpRequest.name()),
                anyString(),
                eq(true)
        );
    }

    @Test
    @DisplayName("Should return 400 Bad Request and validation errors when data is invalid.")
    void shouldReturn400WhenValidationFails() throws Exception {
        SignUpRequest signUpRequest = UserFactory.createSignUpRequest("", "invalid-email", "1", BigDecimal.ZERO);
        performJsonRequest(post(BASE_URL + "/signup"), signUpRequest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists())
                .andExpect(jsonPath("$.validationErrors.unitValue").exists());

        assertThat(userRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return 409 Conflict when attempting to sign up an existing email.")
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        User existingUser = UserFactory.createUser();
        userRepository.save(existingUser);

        SignUpRequest secondUser = UserFactory.createSignUpRequest(existingUser.getEmail());
        performJsonRequest(post(BASE_URL + "/signup"), secondUser)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("The email address is already registered."));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return 200 OK with a JWT and summary user information when signing in with valid credentials.")
    void shouldSignInSuccessfully() throws Exception{
        User user = UserFactory.createUser();
        userRepository.save(user);

        SignInRequest signInRequest = UserFactory.createSignInRequest(user.getEmail(), UserFactory.DEFAULT_PASSWORD);

        performJsonRequest(post(BASE_URL + "/signin"), signInRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.name").value(user.getName()))
                .andExpect(jsonPath("$.user.email").value(user.getEmail()))
                .andExpect(jsonPath("$.user.unitValue").value(user.getUnitValue().doubleValue()))
                .andExpect(jsonPath("$.user.userType").value(UserType.FREE.name()))
                .andExpect(jsonPath("$.user.verified").value(Boolean.FALSE));

        assertThat(refreshTokenRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return 401 Unauthorized and increment failed attempts when password is incorrect.")
    void shouldReturn401AndIncrementAttemptsOnBadPassword() throws Exception {
        User user = UserFactory.createUser();
        userRepository.save(user);

        SignInRequest signInRequest = new SignInRequest(user.getEmail(), "incorrect-password");

        performJsonRequest(post(BASE_URL + "/signin"), signInRequest)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(updatedUser.getLockoutEnd()).isNull();
    }

    @Test
    @DisplayName("Should lock account and return 401 when reaching 5 failed attempts.")
    void shouldLockAccountOnFifthFailedAttempt() throws Exception {
        User user = UserFactory.createUser();
        user.setFailedLoginAttempts(4);
        userRepository.save(user);

        SignInRequest signInRequest = new SignInRequest(user.getEmail(), "incorrect-password");

        performJsonRequest(post(BASE_URL + "/signin"), signInRequest)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account locked due to too many failed attempts. Please try again in 15 minutes."));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(updatedUser.getLockoutEnd()).isNotNull();
    }

    @Test
    @DisplayName("Should block login completely if account is currently locked, even with correct credentials.")
    void shouldBlockLoginWhenAccountIsLocked() throws Exception {
        User user = UserFactory.createUser();
        user.setLockoutEnd(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        SignInRequest signInRequest = UserFactory.createSignInRequest(user.getEmail(), UserFactory.DEFAULT_PASSWORD);

        performJsonRequest(post(BASE_URL + "/signin"), signInRequest)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Account is locked due to too many failed attempts.")));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getLockoutEnd()).isNotNull();
    }

    @Test
    @DisplayName("Should reset failed login attempts to 0 upon successful sign in.")
    void shouldResetFailedAttemptsOnSuccessfulLogin() throws Exception {
        User user = UserFactory.createUser();
        user.setFailedLoginAttempts(3);
        userRepository.save(user);

        SignInRequest signInRequest = UserFactory.createSignInRequest(user.getEmail(), UserFactory.DEFAULT_PASSWORD);

        performJsonRequest(post(BASE_URL + "/signin"), signInRequest)
                .andExpect(status().isOk());

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(updatedUser.getLockoutEnd()).isNull();
    }

    @Test
    @DisplayName("Should return 403 Forbidden when user is inactive.")
    void shouldReturn403WhenUserIsInactive() throws Exception {
        User user = UserFactory.createUser();
        user.setActive(false);
        userRepository.save(user);

        SignInRequest request = UserFactory.createSignInRequest(user.getEmail(), UserFactory.DEFAULT_PASSWORD);

        performJsonRequest(post("/api/v1/auth/signin"), request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Your account has been deactivated. Please contact support to reactivate it."));
    }

    @Test
    @DisplayName("Should verify email successfully and return 204 No Content when provided with a valid OTP.")
    void shouldVerifyEmailSuccessfully() throws Exception{
        User savedUser = userRepository.save(UserFactory.createUser());
        otpTokenRepository.save(OtpTokenFactory.createEmailVerification(savedUser));

        EmailVerificationRequest emailVerificationRequest = new EmailVerificationRequest(OtpTokenFactory.DEFAULT_CODE);

        performAuthenticatedJsonRequest(post(BASE_URL + "/email-verification"), savedUser, emailVerificationRequest)
                .andExpect(status().isNoContent());

        User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.isVerified()).isTrue();
    }

    @Test
    @DisplayName("Should fail verification and return 400 Bad Request when OTP is invalid.")
    void shouldFailVerificationWithInvalidOtp() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());
        otpTokenRepository.save(OtpTokenFactory.createEmailVerification(savedUser));

        EmailVerificationRequest emailVerificationRequest = new EmailVerificationRequest("111111");

        performAuthenticatedJsonRequest(post(BASE_URL + "/email-verification"), savedUser, emailVerificationRequest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid verification code. You have 2 attempt(s) left."));

        User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.isVerified()).isFalse();
    }

    @Test
    @DisplayName("Should burn the OTP token and return 400 when reaching 3 failed attempts.")
    void shouldBurnOtpOnThirdFailedAttempt() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());

        OtpToken token = OtpTokenFactory.createEmailVerification(savedUser);
        token.setFailedAttempts(2);
        otpTokenRepository.save(token);

        EmailVerificationRequest emailVerificationRequest = new EmailVerificationRequest("111111");

        performAuthenticatedJsonRequest(post(BASE_URL + "/email-verification"), savedUser, emailVerificationRequest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Too many failed attempts. This code has been invalidated. Please request a new one."));

        OtpToken burnedToken = otpTokenRepository.findById(token.getId()).orElseThrow();
        assertThat(burnedToken.getFailedAttempts()).isEqualTo(3);
        assertThat(burnedToken.getExpiresAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should resend email verification and return 204 No Content for an unverified user.")
    void shouldResendEmailVerificationSuccessfully() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());

        performAuthenticatedJsonRequest(post(BASE_URL + "/email-verification/resend"), savedUser, null)
                .andExpect(status().isNoContent());

        verify(emailService, times(1)).sendVerificationEmail(
                eq(savedUser.getEmail()),
                eq(savedUser.getName()),
                anyString(),
                eq(false)
        );
    }

    @Test
    @DisplayName("Should return 409 Conflict when attempting to resend verification to an already verified user.")
    void shouldReturn409WhenResendingToVerifiedUser() throws Exception {
        User user = UserFactory.createUser();
        user.setVerified(true);
        User savedUser = userRepository.save(user);

        performAuthenticatedJsonRequest(post(BASE_URL + "/email-verification/resend"), savedUser, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User is already verified."));

        verify(emailService, never()).sendVerificationEmail(
                anyString(),
                anyString(),
                anyString(),
                anyBoolean()
        );
    }

    @Test
    @DisplayName("Should return 204 No Content and trigger email when forgot password is requested for an existing user.")
    void shouldTriggerForgotPasswordSuccessfully() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());
        ForgotPasswordRequest request = new ForgotPasswordRequest(savedUser.getEmail());

        performJsonRequest(post(BASE_URL + "/forgot-password"), request)
                .andExpect(status().isNoContent());

        verify(emailService, times(1)).sendPasswordResetEmail(eq(savedUser.getEmail()), eq(savedUser.getName()), anyString());
    }

    @Test
    @DisplayName("Should return 204 No Content but NOT trigger email when forgot password is requested for a non-existent email.")
    void shouldNotTriggerEmailForUnknownUserOnForgotPassword() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("doesnotexist@example.com");

        performJsonRequest(post(BASE_URL + "/forgot-password"), request)
                .andExpect(status().isNoContent());

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should reset password, trigger notice email, and return 204 No Content when provided with valid data.")
    void shouldResetPasswordSuccessfully() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());
        otpTokenRepository.save(OtpTokenFactory.createResetPassword(savedUser));

        ResetPasswordRequest request = new ResetPasswordRequest(savedUser.getEmail(), OtpTokenFactory.DEFAULT_CODE, "new" + UserFactory.DEFAULT_PASSWORD);

        performJsonRequest(post(BASE_URL + "/reset-password"), request)
                .andExpect(status().isNoContent());

        User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.getPassword()).isNotEqualTo(savedUser.getPassword());

        verify(emailService, times(1)).sendPasswordChangeNotice(savedUser.getEmail(), savedUser.getName());
    }

    @Test
    @DisplayName("Should return 404 Not Found and match missing OTP error when email does not exist on reset password.")
    void shouldReturn404OnResetPasswordWhenEmailDoesNotExist() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("nobody@example.com", OtpTokenFactory.DEFAULT_CODE, "new" + UserFactory.DEFAULT_PASSWORD);

        performJsonRequest(post(BASE_URL + "/reset-password"), request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No verification code was requested."));

        verify(emailService, never()).sendPasswordChangeNotice(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when OTP is invalid on reset password.")
    void shouldReturn400OnResetPasswordWhenOtpIsInvalid() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());
        otpTokenRepository.save(OtpTokenFactory.createResetPassword(savedUser));

        ResetPasswordRequest request = new ResetPasswordRequest(savedUser.getEmail(), "111111", "new" + UserFactory.DEFAULT_PASSWORD);

        performJsonRequest(post(BASE_URL + "/reset-password"), request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid verification code. You have 2 attempt(s) left."));

        verify(emailService, never()).sendPasswordChangeNotice(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when new password matches the current password on reset password.")
    void shouldReturn400OnResetPasswordWhenPasswordIsSame() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());
        otpTokenRepository.save(OtpTokenFactory.createResetPassword(savedUser));;

        ResetPasswordRequest request = new ResetPasswordRequest(savedUser.getEmail(), OtpTokenFactory.DEFAULT_CODE, UserFactory.DEFAULT_PASSWORD);

        performJsonRequest(post(BASE_URL + "/reset-password"), request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password cannot be the same as your current password."));

        verify(emailService, never()).sendPasswordChangeNotice(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 200 OK with new tokens on successful refresh.")
    void shouldReturn200OnRefresh() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());
        refreshTokenRepository.save(RefreshTokenFactory.createValidToken(savedUser));

        RefreshTokenRequest request = new RefreshTokenRequest(RefreshTokenFactory.VALID_RAW_TOKEN);

        performJsonRequest(post(BASE_URL + "/refresh"), request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 204 No Content on logout.")
    void shouldReturn204OnLogout() throws Exception {
        User savedUser = userRepository.save(UserFactory.createUser());
        refreshTokenRepository.save(RefreshTokenFactory.createValidToken(savedUser));

        LogoutRequest request = new LogoutRequest(RefreshTokenFactory.VALID_RAW_TOKEN);

        performJsonRequest(post(BASE_URL + "/logout"), request)
                .andExpect(status().isNoContent());

        assertThat(refreshTokenRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should not allow reuse of refresh token after it has been consumed.")
    void shouldNotAllowReuseOfRefreshToken() throws Exception {
        User user = userRepository.save(UserFactory.createUser());
        refreshTokenRepository.save(RefreshTokenFactory.createValidToken(user));

        RefreshTokenRequest request = new RefreshTokenRequest(RefreshTokenFactory.VALID_RAW_TOKEN);

        performJsonRequest(post(BASE_URL + "/refresh"), request)
                .andExpect(status().isOk());

        performJsonRequest(post(BASE_URL + "/refresh"), request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when token format is invalid.")
    void shouldReturn400WhenTokenFormatIsInvalid() throws Exception {
        LogoutRequest request = new LogoutRequest("too-short");

        performJsonRequest(post(BASE_URL + "/logout"), request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.refreshToken").exists());
    }
}