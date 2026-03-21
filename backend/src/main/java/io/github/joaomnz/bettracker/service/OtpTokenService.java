package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.enums.OtpPurpose;
import io.github.joaomnz.bettracker.exception.BusinessRuleException;
import io.github.joaomnz.bettracker.exception.ResourceNotFoundException;
import io.github.joaomnz.bettracker.exception.TooManyRequestsException;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.model.OtpToken;
import io.github.joaomnz.bettracker.repository.OtpTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpTokenService {
    private final OtpTokenRepository otpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    public OtpTokenService(OtpTokenRepository otpTokenRepository, PasswordEncoder passwordEncoder) {
        this.otpTokenRepository = otpTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public String createOtp(User user, OtpPurpose purpose, LocalDateTime expiresAt){
        OtpToken latest = otpTokenRepository.findTopByUserAndPurposeOrderByCreatedAtDesc(user, purpose)
                .orElse(null);

        if (latest != null && latest.getCreatedAt().plusMinutes(1).isAfter(LocalDateTime.now())) {
            throw new TooManyRequestsException("Please wait 1 minute before requesting another code.");
        }

        String code = generateSixDigitCode();

        otpTokenRepository.save(
                new OtpToken(
                        user,
                        passwordEncoder.encode(code),
                        purpose,
                        expiresAt
                )
        );

        return code;
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public void verifyOtp(User user, String code, OtpPurpose purpose){
        OtpToken latest = otpTokenRepository.findTopByUserAndPurposeOrderByCreatedAtDesc(user, purpose)
                .orElseThrow(() -> new ResourceNotFoundException("No verification code was requested."));

        if(latest.getUsedAt() != null){
            throw new BusinessRuleException("This verification code has already been used.");
        }

        LocalDateTime now = LocalDateTime.now();

        if(latest.getExpiresAt().isBefore(now)){
            throw new BusinessRuleException("This verification code has expired.");
        }

        if (!passwordEncoder.matches(code, latest.getCode())) {
            int attempts = latest.getFailedAttempts() + 1;
            latest.setFailedAttempts(attempts);

            if(attempts >= 3){
                latest.setExpiresAt(now);
                throw new BusinessRuleException("Too many failed attempts. This code has been invalidated. Please request a new one.");
            }

            throw new BusinessRuleException("Invalid verification code. You have " + (3 - attempts) + " attempt(s) left.");
        }

        latest.setUsedAt(now);
    }

    private String generateSixDigitCode(){
        int num = secureRandom.nextInt(1000000);
        return String.format("%06d", num);
    }
}