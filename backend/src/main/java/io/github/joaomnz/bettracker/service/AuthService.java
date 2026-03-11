package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.*;
import io.github.joaomnz.bettracker.enums.OtpPurpose;
import io.github.joaomnz.bettracker.exception.BusinessRuleException;
import io.github.joaomnz.bettracker.exception.DataConflictException;
import io.github.joaomnz.bettracker.exception.ResourceNotFoundException;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.repository.UserRepository;
import io.github.joaomnz.bettracker.security.JwtService;
import io.github.joaomnz.bettracker.security.UserDetailsImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final OtpTokenService otpTokenService;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService, OtpTokenService otpTokenService, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.otpTokenService = otpTokenService;
        this.emailService = emailService;
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

        String otp = otpTokenService.createOtp(
                savedUser,
                OtpPurpose.EMAIL_VERIFICATION,
                LocalDateTime.now().plusHours(24)
        );

        emailService.sendVerificationEmail(savedUser.getEmail(), otp);

        return new AuthResponse(
                jwtService.generateToken(new UserDetailsImpl(savedUser)),
                UserResponse.fromEntity(savedUser)
        );
    }

    @Transactional
    public AuthResponse signIn(SignInRequest request){
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessRuleException("Invalid credentials."));

        LocalDateTime now = LocalDateTime.now();

        if(user.getLockoutEnd() != null && user.getLockoutEnd().isAfter(now)){
            long minutesLeft = ChronoUnit.MINUTES.between(now, user.getLockoutEnd());
            throw new LockedException("Account is locked due to too many failed attempts. Please try again in " + Math.max(1, minutesLeft) + " minute(s).");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            user.setFailedLoginAttempts(0);
            user.setLockoutEnd(null);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            return new AuthResponse(
                    jwtService.generateToken(userDetails),
                    UserResponse.fromEntity(userDetails.getUser())
            );

        } catch(BadCredentialsException exception) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if(attempts >= 5){
                user.setLockoutEnd(now.plusMinutes(15));
                user.setFailedLoginAttempts(0);
                throw new LockedException("Account locked due to too many failed attempts. Please try again in 15 minutes.");
            }

            throw exception;

        } finally {
            userRepository.save(user);
        }
    }

    @Transactional
    public void verifyEmail(User user, EmailVerificationRequest request){
        if(user.isVerified()){
            throw new DataConflictException("User is already verified.");
        }

        otpTokenService.verifyOtp(user, request.otp(), OtpPurpose.EMAIL_VERIFICATION);

        user.setVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public void resendEmailVerification(User user){
        if(user.isVerified()){
            throw new DataConflictException("User is already verified.");
        }

        String otp = otpTokenService.createOtp(
                user,
                OtpPurpose.EMAIL_VERIFICATION,
                LocalDateTime.now().plusHours(24)
        );

        emailService.sendVerificationEmail(user.getEmail(), otp);
    }
}
