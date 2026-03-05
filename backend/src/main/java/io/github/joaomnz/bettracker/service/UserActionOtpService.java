package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.enums.ActionType;
import io.github.joaomnz.bettracker.exception.BusinessRuleException;
import io.github.joaomnz.bettracker.exception.ResourceNotFound;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.model.UserActionOtp;
import io.github.joaomnz.bettracker.repository.UserActionOtpRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class UserActionOtpService {
    private final UserActionOtpRepository userActionOtpRepository;
    private final PasswordEncoder passwordEncoder;

    public UserActionOtpService(UserActionOtpRepository userActionOtpRepository, PasswordEncoder passwordEncoder) {
        this.userActionOtpRepository = userActionOtpRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String generateOtp(User user, ActionType actionType, LocalDateTime expiresAt){
        String otp = generateSixDigitCode();

        userActionOtpRepository.save(
                new UserActionOtp(
                        user,
                        passwordEncoder.encode(otp),
                        actionType,
                        expiresAt
                )
        );

        return otp;
    }

    public void validateAndBurnOtp(User user, String otp, ActionType actionType){
        UserActionOtp storedOtp = userActionOtpRepository.findTopByUserAndActionTypeOrderByCreatedAtDesc(user, actionType)
                .orElseThrow(() -> new ResourceNotFound("No OTP found."));

        if(storedOtp.getUsedAt() != null || storedOtp.getExpiresAt().isBefore(LocalDateTime.now())){
            throw new BusinessRuleException("This code has expired or was already used.");
        }

        if (!passwordEncoder.matches(otp, storedOtp.getOtp())) {
            throw new BusinessRuleException("Invalid code.");
        }

        storedOtp.setUsedAt(LocalDateTime.now());
        userActionOtpRepository.save(storedOtp);
    }

    private String generateSixDigitCode(){
        SecureRandom secureRandom = new SecureRandom();
        int num = secureRandom.nextInt(1000000);
        return String.format("%06d", num);
    }
}