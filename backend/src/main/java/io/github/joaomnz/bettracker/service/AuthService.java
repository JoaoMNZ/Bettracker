package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.*;
import io.github.joaomnz.bettracker.enums.ActionType;
import io.github.joaomnz.bettracker.exception.BusinessRuleException;
import io.github.joaomnz.bettracker.exception.DataConflictException;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.repository.UserRepository;
import io.github.joaomnz.bettracker.security.TokenService;
import io.github.joaomnz.bettracker.security.UserDetailsImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserActionOtpService userActionOtpService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, TokenService tokenService, UserActionOtpService userActionOtpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.userActionOtpService = userActionOtpService;
    }

    @Transactional
    public AuthResponse signUp(SignUpRequest request){
        if(userRepository.existsByEmail(request.email())){
            throw new DataConflictException("The email address is already registered.");
        }

        User savedUser = userRepository.save(
                new User(
                    request.name(),
                    request.email(),
                    passwordEncoder.encode(request.password()),
                    request.unitValue()
                )
        );

        String otp = userActionOtpService.generateOtp(
                savedUser,
                ActionType.EMAIL_VERIFICATION,
                LocalDateTime.now().plusHours(24)
        );

        // We need to implement an SMTP to actually send the token to the user's email.

        return new AuthResponse(
                tokenService.generateToken(new UserDetailsImpl(savedUser)),
                UserResponse.fromEntity(savedUser)
        );
    }

    public AuthResponse signIn(SignInRequest request){
        UsernamePasswordAuthenticationToken authToken  = new UsernamePasswordAuthenticationToken(request.email(), request.password());
        Authentication authentication = authenticationManager.authenticate(authToken);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return new AuthResponse(
                tokenService.generateToken(userDetails),
                UserResponse.fromEntity(userDetails.getUser())
        );
    }

    @Transactional
    public void verifyEmail(User user, VerifyEmailRequest request){
        if(user.isVerified()){
            throw new DataConflictException("User is already verified.");
        }

        userActionOtpService.validateAndBurnOtp(user, request.otp(), ActionType.EMAIL_VERIFICATION);

        user.setVerified(true);
        userRepository.save(user);
    }
}
