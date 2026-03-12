package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.DeactivateAccountRequest;
import io.github.joaomnz.bettracker.dto.UpdatePasswordRequest;
import io.github.joaomnz.bettracker.dto.UpdateProfileRequest;
import io.github.joaomnz.bettracker.dto.UserProfileResponse;
import io.github.joaomnz.bettracker.exception.BusinessRuleException;
import io.github.joaomnz.bettracker.exception.ResourceNotFoundException;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
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
