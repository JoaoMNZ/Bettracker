package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.exception.BusinessRuleException;
import io.github.joaomnz.bettracker.model.RefreshToken;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.repository.RefreshTokenRepository;
import io.github.joaomnz.bettracker.util.TokenUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public String generateToken(User user){
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserOrderByExpiresAtAsc(user);

        if(activeTokens.size() >= 5){
            refreshTokenRepository.delete(activeTokens.getFirst());
        }

        String rawToken = TokenUtil.generateRawToken();

        refreshTokenRepository.save(
                new RefreshToken(
                        user,
                        TokenUtil.hashToken(rawToken),
                        LocalDateTime.now().plusDays(7)
                )
        );

        return rawToken;
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public User consumeToken(String rawToken){
        RefreshToken refreshToken = refreshTokenRepository.findByToken(TokenUtil.hashToken(rawToken))
                .orElseThrow(() -> new BusinessRuleException("Invalid refresh token."));

        if(refreshToken.getExpiresAt().isBefore(LocalDateTime.now())){
            refreshTokenRepository.delete(refreshToken);
            throw new BusinessRuleException("Refresh token has expired. Please sign in again.");
        }

        refreshTokenRepository.delete(refreshToken);

        return refreshToken.getUser();
    }

    @Transactional
    public void revokeToken(String rawToken){
        refreshTokenRepository.findByToken(TokenUtil.hashToken(rawToken))
                .ifPresent(refreshTokenRepository::delete);
    }
}
