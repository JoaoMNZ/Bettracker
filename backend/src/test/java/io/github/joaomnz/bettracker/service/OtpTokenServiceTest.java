package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.enums.OtpPurpose;
import io.github.joaomnz.bettracker.exception.BusinessRuleException;
import io.github.joaomnz.bettracker.exception.ResourceNotFoundException;
import io.github.joaomnz.bettracker.exception.TooManyRequestsException;
import io.github.joaomnz.bettracker.factory.OtpTokenFactory;
import io.github.joaomnz.bettracker.factory.UserFactory;
import io.github.joaomnz.bettracker.model.OtpToken;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.repository.OtpTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OtpTokenServiceTest {
    @Mock
    private OtpTokenRepository otpTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OtpTokenService otpTokenService;

    private User testUser;

    @BeforeEach
    void setUp(){
        testUser = UserFactory.createUser();
    }

    @Test
    @DisplayName("Should generate, hash, and save a 6-digit OTP when no recent token exists.")
    void shouldCreateOtpSuccessfullyWhenNoRecentTokenExists(){
        when(otpTokenRepository.findTopByUserAndPurposeOrderByCreatedAtDesc(testUser, OtpPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");

        String rawCode = otpTokenService.createOtp(testUser, OtpPurpose.EMAIL_VERIFICATION, LocalDateTime.now().plusHours(24));
        assertThat(rawCode).hasSize(6).containsOnlyDigits();
        verify(otpTokenRepository, times(1)).save(any(OtpToken.class));
    }

    @Test
    @DisplayName("Should generate, hash, and save a 6-digit OTP when last token is older than one minute.")
    void shouldCreateOtpSuccessfullyWhenLastTokenIsOlderThanOneMinute(){
        OtpToken oldToken = OtpTokenFactory.createEmailVerification(testUser);
        oldToken.setCreatedAt(LocalDateTime.now().minusSeconds(61));

        when(otpTokenRepository.findTopByUserAndPurposeOrderByCreatedAtDesc(testUser, OtpPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(oldToken));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");

        String rawCode = otpTokenService.createOtp(testUser, OtpPurpose.EMAIL_VERIFICATION, LocalDateTime.now().plusHours(24));
        assertThat(rawCode).hasSize(6).containsOnlyDigits();
        verify(otpTokenRepository, times(1)).save(any(OtpToken.class));
    }

    @Test
    @DisplayName("Should throw TooManyRequestsException when requesting an OTP before 1 minute has passed.")
    void shouldThrowExceptionWhenRateLimited(){
        OtpToken recentOtp = OtpTokenFactory.createEmailVerification(testUser);
        recentOtp.setCreatedAt(LocalDateTime.now().minusSeconds(30));

        when(otpTokenRepository.findTopByUserAndPurposeOrderByCreatedAtDesc(testUser, OtpPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(recentOtp));

        assertThatThrownBy(() -> otpTokenService.createOtp(testUser, OtpPurpose.EMAIL_VERIFICATION, LocalDateTime.now().plusHours(24)))
                .isInstanceOf(TooManyRequestsException.class);

        verify(otpTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully verify and update the OTP token.")
    void shouldVerifyOtpSuccessfully() {
        OtpToken validOtp = OtpTokenFactory.createEmailVerification(testUser);

        when(otpTokenRepository.findTopByUserAndPurposeOrderByCreatedAtDesc(testUser, OtpPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(validOtp));

        when(passwordEncoder.matches(OtpTokenFactory.DEFAULT_CODE, validOtp.getCode()))
                .thenReturn(true);

        otpTokenService.verifyOtp(testUser, OtpTokenFactory.DEFAULT_CODE, OtpPurpose.EMAIL_VERIFICATION);

        assertThat(validOtp.getUsedAt()).isNotNull();
        verify(otpTokenRepository, times(1)).save(validOtp);
    }

    @Test
    @DisplayName("Should throw BusinessRuleException and increment failed attempts when the provided code is invalid.")
    void shouldThrowExceptionWhenCodeIsInvalid() {
        OtpToken validOtp = OtpTokenFactory.createEmailVerification(testUser);

        when(otpTokenRepository.findTopByUserAndPurposeOrderByCreatedAtDesc(testUser, OtpPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(validOtp));

        String wrongCode = "111111";
        when(passwordEncoder.matches(wrongCode, validOtp.getCode()))
                .thenReturn(false);

        assertThatThrownBy(() -> otpTokenService.verifyOtp(testUser, wrongCode, OtpPurpose.EMAIL_VERIFICATION))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Invalid verification code. You have 2 attempt(s) left.");

        assertThat(validOtp.getFailedAttempts()).isEqualTo(1);
        verify(otpTokenRepository, times(1)).save(validOtp);
    }

    @Test
    @DisplayName("Should burn the token and throw BusinessRuleException when reaching 3 failed attempts.")
    void shouldBurnTokenOnThirdFailedAttempt() throws Exception {
        OtpToken otp = OtpTokenFactory.createEmailVerification(testUser);
        otp.setFailedAttempts(2);

        when(otpTokenRepository.findTopByUserAndPurposeOrderByCreatedAtDesc(testUser, OtpPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(otp));

        String wrongCode = "111111";
        when(passwordEncoder.matches(wrongCode, otp.getCode()))
                .thenReturn(false);

        assertThatThrownBy(() -> otpTokenService.verifyOtp(testUser, wrongCode, OtpPurpose.EMAIL_VERIFICATION))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Too many failed attempts. This code has been invalidated. Please request a new one.");

        assertThat(otp.getFailedAttempts()).isEqualTo(3);
        assertThat(otp.getExpiresAt()).isBeforeOrEqualTo(LocalDateTime.now());
        verify(otpTokenRepository, times(1)).save(otp);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when no token exists for the user.")
    void shouldThrowExceptionWhenNoTokenExists() {
        when(otpTokenRepository.findTopByUserAndPurposeOrderByCreatedAtDesc(testUser, OtpPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpTokenService.verifyOtp(testUser, OtpTokenFactory.DEFAULT_CODE, OtpPurpose.EMAIL_VERIFICATION))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(otpTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when the token has already been used.")
    void shouldThrowExceptionWhenTokenAlreadyUsed() {
        OtpToken usedOtp = OtpTokenFactory.createEmailVerification(testUser);
        usedOtp.setUsedAt(LocalDateTime.now().minusMinutes(5));

        when(otpTokenRepository.findTopByUserAndPurposeOrderByCreatedAtDesc(testUser, OtpPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(usedOtp));

        assertThatThrownBy(() -> otpTokenService.verifyOtp(testUser, OtpTokenFactory.DEFAULT_CODE, OtpPurpose.EMAIL_VERIFICATION))
                .isInstanceOf(BusinessRuleException.class);

        verify(otpTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when the token is expired.")
    void shouldThrowExceptionWhenTokenIsExpired() {
        OtpToken expiredOtp = OtpTokenFactory.createEmailVerification(testUser);
        expiredOtp.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(otpTokenRepository.findTopByUserAndPurposeOrderByCreatedAtDesc(testUser, OtpPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(expiredOtp));

        assertThatThrownBy(() -> otpTokenService.verifyOtp(testUser, OtpTokenFactory.DEFAULT_CODE, OtpPurpose.EMAIL_VERIFICATION))
                .isInstanceOf(BusinessRuleException.class);

        verify(otpTokenRepository, never()).save(any());
    }
}
