package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.user.*;
import io.github.joaomnz.bettracker.enums.OtpPurpose;
import io.github.joaomnz.bettracker.exception.BusinessRuleException;
import io.github.joaomnz.bettracker.exception.DataConflictException;
import io.github.joaomnz.bettracker.exception.ResourceNotFoundException;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OtpTokenService otpTokenService;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId){
        User user = getUserById(userId);
        return UserProfileResponse.fromEntity(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request){
        User user = getUserById(userId);

        if(request.name() != null) user.setName(request.name());
        if(request.unitValue() != null) user.setUnitValue(request.unitValue());

        return UserProfileResponse.fromEntity(user);
    }

    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest request){
        User user = getUserById(userId);

        if(user.getPassword() == null){
            throw new BusinessRuleException("This account does not have a password.");
        }

        if(!passwordEncoder.matches(request.oldPassword(), user.getPassword())){
            throw new BusinessRuleException("Incorrect current password.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessRuleException("New password cannot be the same as your current one.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        emailService.notifyPasswordChange(user.getEmail(), user.getName());
    }

    @Transactional
    public void setPassword(Long userId, SetPasswordRequest request){
        User user = getUserById(userId);

        if(user.getPassword() != null){
            throw new BusinessRuleException("You already have a password.");
        }

        user.setPassword(passwordEncoder.encode(request.password()));
        emailService.notifyPasswordSet(user.getEmail(), user.getName());
    }

    @Transactional
    public void requestEmailChange(Long userId, RequestEmailChangeRequest request){
        User user = getUserById(userId);

        if (user.getPassword() != null) {
            if (request.currentPassword() == null ||!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                throw new BusinessRuleException("Incorrect password.");
            }
        }

        String normalizedNewEmail = request.newEmail().trim().toLowerCase();

        if(user.getEmail().equals(normalizedNewEmail)){
            throw new BusinessRuleException("The new email must be different from your current one.");
        }

        if(userRepository.existsByEmail(normalizedNewEmail)){
            throw new DataConflictException("The email address is already registered.");
        }

        user.setPendingEmail(normalizedNewEmail);

        String otp = otpTokenService.createOtp(user, OtpPurpose.EMAIL_CHANGE, LocalDateTime.now().plusMinutes(15));
        emailService.sendEmailChangeCode(normalizedNewEmail, user.getName(), otp);
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public void verifyEmailChange(Long userId, VerifyEmailChangeRequest request){
        User user = getUserById(userId);

        if (user.getPendingEmail() == null) {
            throw new BusinessRuleException("No email change request is currently pending.");
        }

        otpTokenService.verifyOtp(user, request.otp(), OtpPurpose.EMAIL_CHANGE);

        String oldEmail = user.getEmail();
        user.setEmail(user.getPendingEmail());
        user.setPendingEmail(null);
        user.setVerified(true);

        emailService.notifyEmailChange(oldEmail, user.getName());
    }

    @Transactional
    public void deactivateAccount(Long userId, DeactivateAccountRequest request){
        User user = getUserById(userId);

        if(!user.isActive()){
            throw new BusinessRuleException("Account is already deactivated.");
        }

        if (user.getPassword() != null) {
            if (request.password() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
                throw new BusinessRuleException("Incorrect password.");
            }
        }

        user.setActive(false);
        emailService.notifyAccountDeactivation(user.getEmail(), user.getName());
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}