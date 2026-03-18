package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.exception.BusinessRuleException;
import io.github.joaomnz.bettracker.model.RefreshToken;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.repository.RefreshTokenRepository;
import io.github.joaomnz.bettracker.util.TokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private RefreshToken validToken;
    private RefreshToken expiredToken;

    private final String rawTokenString = "A".repeat(43);
    private final String expectedHash = TokenUtil.hashToken(rawTokenString);

    @BeforeEach
    void setUp() {
        testUser = new User();
        validToken = new RefreshToken(testUser, expectedHash, LocalDateTime.now().plusDays(7));
        expiredToken = new RefreshToken(testUser, expectedHash, LocalDateTime.now().minusDays(1));
    }

    @Test
    @DisplayName("Should generate, hash, and save token without deleting when user has under 5 tokens.")
    void shouldGenerateTokenWhenUnderLimit() {
        when(refreshTokenRepository.findAllByUserOrderByExpiresAtAsc(testUser)).thenReturn(List.of());

        String rawResult = refreshTokenService.generateToken(testUser);

        assertThat(rawResult).isNotNull().hasSize(43);
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(1)).save(captor.capture());

        RefreshToken savedToken = captor.getValue();
        assertThat(savedToken.getToken()).isEqualTo(TokenUtil.hashToken(rawResult));
        assertThat(savedToken.getToken()).isNotEqualTo(rawResult);
    }

    @Test
    @DisplayName("Should delete the oldest token before saving when user hits the 5-token limit.")
    void shouldDeleteOldestTokenWhenAtLimit() {
        RefreshToken oldestToken = new RefreshToken(testUser, "oldest", LocalDateTime.now().minusDays(3));
        List<RefreshToken> activeTokens = List.of(
                oldestToken,
                new RefreshToken(), new RefreshToken(), new RefreshToken(), new RefreshToken()
        );

        when(refreshTokenRepository.findAllByUserOrderByExpiresAtAsc(testUser)).thenReturn(activeTokens);

        refreshTokenService.generateToken(testUser);

        verify(refreshTokenRepository, times(1)).delete(oldestToken);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should hash input, find token, return user, and delete token.")
    void shouldConsumeValidTokenSuccessfully() {
        when(refreshTokenRepository.findByToken(expectedHash)).thenReturn(Optional.of(validToken));

        User resultUser = refreshTokenService.consumeToken(rawTokenString);

        assertThat(resultUser).isEqualTo(testUser);
        verify(refreshTokenRepository, times(1)).delete(validToken);
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when token hash does not exist.")
    void shouldThrowExceptionWhenTokenNotFound() {
        when(refreshTokenRepository.findByToken(expectedHash)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.consumeToken(rawTokenString))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Invalid refresh token.");

        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw BusinessRuleException and delete token when it is expired.")
    void shouldThrowExceptionAndDeleteWhenTokenIsExpired() {
        when(refreshTokenRepository.findByToken(expectedHash)).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.consumeToken(rawTokenString))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Refresh token has expired. Please sign in again.");

        verify(refreshTokenRepository, times(1)).delete(expiredToken);
    }

    @Test
    @DisplayName("Should hash input and delete token if it exists when revoked.")
    void shouldDeleteTokenOnRevoke() {
        when(refreshTokenRepository.findByToken(expectedHash)).thenReturn(Optional.of(validToken));

        refreshTokenService.revokeToken(rawTokenString);

        verify(refreshTokenRepository, times(1)).delete(validToken);
    }

    @Test
    @DisplayName("Should do nothing if token hash does not exist when revoked.")
    void shouldDoNothingWhenRevokingNonExistentToken() {
        when(refreshTokenRepository.findByToken(expectedHash)).thenReturn(Optional.empty());

        refreshTokenService.revokeToken(rawTokenString);

        verify(refreshTokenRepository, never()).delete(any());
    }
}