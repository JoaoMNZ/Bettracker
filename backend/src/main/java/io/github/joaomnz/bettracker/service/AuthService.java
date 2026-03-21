package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.auth.*;
import io.github.joaomnz.bettracker.dto.user.ForgotPasswordRequest;
import io.github.joaomnz.bettracker.dto.user.ResetPasswordRequest;
import io.github.joaomnz.bettracker.dto.user.UserResponse;
import io.github.joaomnz.bettracker.enums.OtpPurpose;
import io.github.joaomnz.bettracker.exception.BusinessRuleException;
import io.github.joaomnz.bettracker.exception.DataConflictException;
import io.github.joaomnz.bettracker.exception.ResourceNotFoundException;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.repository.UserRepository;
import io.github.joaomnz.bettracker.security.JwtProvider;
import io.github.joaomnz.bettracker.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final OtpTokenService otpTokenService;
    private final EmailService emailService;
    private final GoogleAuthService googleAuthService;

    @Transactional
    public AuthResponse signUp(SignUpRequest request){
        String normalizedEmail = request.email().trim().toLowerCase();

        if(userRepository.existsByEmail(normalizedEmail)){
            throw new DataConflictException("The email address is already registered.");
        }

        User savedUser = userRepository.save(
                User.builder()
                        .name(request.name())
                        .email(normalizedEmail)
                        .password(passwordEncoder.encode(request.password()))
                        .build()
        );

        String otp = otpTokenService.createOtp(
                savedUser,
                OtpPurpose.EMAIL_VERIFICATION,
                LocalDateTime.now().plusHours(24)
        );

        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getName(), otp);

        return buildAuthResponse(savedUser);
    }

    @Transactional(noRollbackFor = {BadCredentialsException.class, LockedException.class})
    public AuthResponse signIn(SignInRequest request){
        String normalizedEmail = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessRuleException("Invalid credentials."));

        LocalDateTime now = LocalDateTime.now();

        if(user.getLockoutEnd() != null && user.getLockoutEnd().isAfter(now)){
            long minutesLeft = ChronoUnit.MINUTES.between(now, user.getLockoutEnd());
            throw new LockedException("Account is locked due to too many failed attempts. Please try again in " + Math.max(1, minutesLeft) + " minute(s).");
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(normalizedEmail, request.password()));
            user.setFailedLoginAttempts(0);
            user.setLockoutEnd(null);

            return buildAuthResponse(user);

        } catch(BadCredentialsException exception) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if(attempts >= 5){
                user.setLockoutEnd(now.plusMinutes(15));
                user.setFailedLoginAttempts(0);
                throw new LockedException("Account locked due to too many failed attempts. Please try again in 15 minutes.");
            }
            throw exception;
        }
    }

    public AuthResponse authenticateWithGoogle(GoogleLoginRequest request) {
        GoogleUserInfo googleInfo = googleAuthService.verifyToken(request.token());

        User user = userRepository.findByGoogleId(googleInfo.googleId())
                .orElseGet(() -> linkOrCreateUser(googleInfo));

        if (!user.isActive()) {
            throw new DisabledException("Your account is deactivated. Contact support.");
        }

        user.setFailedLoginAttempts(0);
        user.setLockoutEnd(null);
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public AuthResponse refresh(RefreshTokenRequest request){
        User user = refreshTokenService.consumeToken(request.refreshToken());
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(LogoutRequest request){
        refreshTokenService.revokeToken(request.refreshToken());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request){
        String normalizedEmail = request.email().trim().toLowerCase();

        userRepository.findByEmail(normalizedEmail)
                .ifPresent(user -> {
                    String otp = otpTokenService.createOtp(
                            user,
                            OtpPurpose.PASSWORD_RESET,
                            LocalDateTime.now().plusMinutes(15)
                    );

                    emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), otp);
                });
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public void resetPassword(ResetPasswordRequest request){
        String normalizedEmail = request.email().trim().toLowerCase();

        // Prevents email enumeration. By mimicking the error for a missing OTP, attackers cannot distinguish between an unregistered email and an inactive reset request.
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No verification code was requested."));

        otpTokenService.verifyOtp(user, request.otp(), OtpPurpose.PASSWORD_RESET);

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessRuleException("New password cannot be the same as your current password.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));

        emailService.sendPasswordChangeNotice(user.getEmail(), user.getName());
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public void verifyEmail(Long userId, EmailVerificationRequest request){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if(user.isVerified()){
            throw new BusinessRuleException("User is already verified.");
        }

        otpTokenService.verifyOtp(user, request.otp(), OtpPurpose.EMAIL_VERIFICATION);

        user.setVerified(true);

        emailService.sendWelcomeEmail(user.getEmail(), user.getName());
    }

    @Transactional
    public void resendEmailVerification(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if(user.isVerified()){
            throw new BusinessRuleException("User is already verified.");
        }

        String otp = otpTokenService.createOtp(
                user,
                OtpPurpose.EMAIL_VERIFICATION,
                LocalDateTime.now().plusHours(24)
        );

        emailService.sendVerificationEmail(user.getEmail(), user.getName(), otp);
    }

    private User linkOrCreateUser(GoogleUserInfo googleInfo) {
        String normalizedEmail = googleInfo.email().trim().toLowerCase();

        return userRepository.findByEmail(normalizedEmail)
                .map(existingUser -> {
                    if (existingUser.getGoogleId() != null && !existingUser.getGoogleId().equals(googleInfo.googleId())) {
                        throw new DataConflictException("Email already linked to another Google identity.");
                    }

                    existingUser.setGoogleId(googleInfo.googleId());
                    if(!existingUser.isVerified()){
                        existingUser.setVerified(true);
                        emailService.sendWelcomeEmail(normalizedEmail, existingUser.getName());
                    }

                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .name(googleInfo.name())
                            .email(normalizedEmail)
                            .googleId(googleInfo.googleId())
                            .verified(true)
                            .build();

                    emailService.sendWelcomeEmail(newUser.getEmail(), newUser.getName());

                    return userRepository.save(newUser);
                });
    }

    private AuthResponse buildAuthResponse(User user) {
        return new AuthResponse(
                refreshTokenService.generateToken(user),
                jwtProvider.generateToken(new UserDetailsImpl(user)),
                UserResponse.fromEntity(user)
        );
    }
}