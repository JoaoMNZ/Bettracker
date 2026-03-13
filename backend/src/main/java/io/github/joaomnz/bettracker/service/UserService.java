package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.user.*;
import io.github.joaomnz.bettracker.enums.OtpPurpose;
import io.github.joaomnz.bettracker.exception.BusinessRuleException;
import io.github.joaomnz.bettracker.exception.DataConflictException;
import io.github.joaomnz.bettracker.exception.ResourceNotFoundException;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OtpTokenService otpTokenService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService, OtpTokenService otpTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.otpTokenService = otpTokenService;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        return UserProfileResponse.fromEntity(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if(request.name() != null){
            user.setName(request.name());
        }

        if(request.unitValue() != null){
            user.setUnitValue(request.unitValue());
        }

        userRepository.saveAndFlush(user);

        return UserProfileResponse.fromEntity(user);
    }

    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest request){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if(!passwordEncoder.matches(request.oldPassword(), user.getPassword())){
            throw new BusinessRuleException("Incorrect current password.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessRuleException("New password cannot be the same as your current password.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));

        emailService.sendPasswordChangeNotice(user.getEmail(), user.getName());
    }

    @Transactional
    public void requestEmailChange(Long userId, RequestEmailChangeRequest request){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if(!passwordEncoder.matches(request.currentPassword(), user.getPassword())){
            throw new BusinessRuleException("Incorrect current password.");
        }

        if(request.newEmail().equalsIgnoreCase(user.getEmail())){
            throw new BusinessRuleException("The new email must be different from your current email.");
        }

        if(userRepository.existsByEmail(request.newEmail())){
            throw new DataConflictException("The email address is already registered.");
        }

        user.setPendingEmail(request.newEmail());

        String otp = otpTokenService.createOtp(user, OtpPurpose.EMAIL_CHANGE, LocalDateTime.now().plusMinutes(15));
        emailService.sendEmailChangeVerificationEmail(request.newEmail(), user.getName(), otp);
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public void verifyEmailChange(Long userId, VerifyEmailChangeRequest request){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (user.getPendingEmail() == null) {
            throw new BusinessRuleException("No email change request is currently pending.");
        }

        otpTokenService.verifyOtp(user, request.otp(), OtpPurpose.EMAIL_CHANGE);

        String oldEmail = user.getEmail();

        user.setEmail(user.getPendingEmail());
        user.setPendingEmail(null);
        user.setVerified(true);

        emailService.sendEmailChangeNotice(oldEmail, user.getName());
    }

    @Transactional
    public void deactivateAccount(Long userId, DeactivateAccountRequest request){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if(!user.isActive()){
            throw new BusinessRuleException("Account is already deactivated.");
        }

        if(!passwordEncoder.matches(request.password(), user.getPassword())){
            throw new BusinessRuleException("Incorrect password.");
        }

        user.setActive(false);
        emailService.sendDeactivationEmail(user.getEmail(), user.getName());
    }
}
