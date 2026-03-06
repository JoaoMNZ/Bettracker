package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.*;
import io.github.joaomnz.bettracker.enums.OtpPurpose;
import io.github.joaomnz.bettracker.exception.DataConflictException;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.repository.UserRepository;
import io.github.joaomnz.bettracker.security.JwtService;
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
    private final JwtService jwtService;
    private final OtpTokenService otpTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService, OtpTokenService otpTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.otpTokenService = otpTokenService;
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

        // We need to implement an SMTP to actually send the token to the user's email.

        return new AuthResponse(
                jwtService.generateToken(new UserDetailsImpl(savedUser)),
                UserResponse.fromEntity(savedUser)
        );
    }

    public AuthResponse signIn(SignInRequest request){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return new AuthResponse(
                jwtService.generateToken(userDetails),
                UserResponse.fromEntity(userDetails.getUser())
        );
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

        // SMTP
    }
}
