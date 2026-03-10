package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.enums.OtpPurpose;
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
}
