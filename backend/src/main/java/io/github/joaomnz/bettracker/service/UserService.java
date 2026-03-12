package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.DeactivateAccountRequest;
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

    @Transactional
    public void deactivateAccount(Long userId, DeactivateAccountRequest request){
        User freshUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if(!freshUser.isActive()){
            throw new BusinessRuleException("Account is already deactivated.");
        }

        if(!passwordEncoder.matches(request.password(), freshUser.getPassword())){
            throw new BusinessRuleException("Incorrect password.");
        }

        freshUser.setActive(false);
        emailService.sendDeactivationEmail(freshUser.getEmail(), freshUser.getName());
    }
}
