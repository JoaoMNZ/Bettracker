package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.auth.*;
import io.github.joaomnz.bettracker.dto.user.ForgotPasswordRequest;
import io.github.joaomnz.bettracker.dto.user.ResetPasswordRequest;
import io.github.joaomnz.bettracker.dto.user.UserResponse;
import io.github.joaomnz.bettracker.enums.AuthProvider;
import io.github.joaomnz.bettracker.enums.OtpPurpose;
import io.github.joaomnz.bettracker.exception.BusinessRuleException;
import io.github.joaomnz.bettracker.exception.DataConflictException;
import io.github.joaomnz.bettracker.exception.ResourceNotFoundException;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.repository.UserRepository;
import io.github.joaomnz.bettracker.security.JwtService;
import io.github.joaomnz.bettracker.security.UserDetailsImpl;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.crypto.Data;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final OtpTokenService otpTokenService;
    private final EmailService emailService;
    private final GoogleAuthService googleAuthService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService, RefreshTokenService refreshTokenService, OtpTokenService otpTokenService, EmailService emailService, GoogleAuthService googleAuthService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.otpTokenService = otpTokenService;
        this.emailService = emailService;
        this.googleAuthService = googleAuthService;
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

        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getName(), otp, true);

        return new AuthResponse(
                refreshTokenService.generateToken(savedUser),
                jwtService.generateToken(new UserDetailsImpl(savedUser)),
                UserResponse.fromEntity(savedUser)
        );
    }

    @Transactional(noRollbackFor = {BadCredentialsException.class, LockedException.class})
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
                    refreshTokenService.generateToken(user),
                    jwtService.generateToken(new UserDetailsImpl(user)),
                    UserResponse.fromEntity(user)
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

    public AuthResponse authenticateWithGoogle(GoogleLoginRequest request){
        GoogleUserInfo googleUserInfo = googleAuthService.verifyToken(request.token());

        return processTransactionalGoogleLogin(googleUserInfo);
    }

    @Transactional
    protected AuthResponse processTransactionalGoogleLogin(GoogleUserInfo googleUserInfo) {
        User user = userRepository.findByGoogleId(googleUserInfo.googleId())
                .orElseGet(() -> handleGoogleMergeOrCreate(googleUserInfo));

        if (!user.isActive()) {
            throw new DisabledException("Your account has been deactivated. Please contact support to reactivate it.");
        }

        return new AuthResponse(
                refreshTokenService.generateToken(user),
                jwtService.generateToken(new UserDetailsImpl(user)),
                UserResponse.fromEntity(user)
        );
    }

    private User handleGoogleMergeOrCreate(GoogleUserInfo googleUserInfo) {
        return userRepository.findByEmail(googleUserInfo.email())
                .map(existingUser -> {
                    if (existingUser.getGoogleId() != null && !existingUser.getGoogleId().equals(googleUserInfo.googleId())) {
                        throw new DataConflictException("This email is already linked to a different Google account.");
                    }

                    if (!existingUser.isVerified()) {
                        existingUser.setVerified(true);
                    }

                    existingUser.setGoogleId(googleUserInfo.googleId());
                    if (existingUser.getPassword() == null) {
                        existingUser.setAuthProvider(AuthProvider.GOOGLE);
                    }

                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    User newUser = new User();

                    String safeName = googleUserInfo.name() != null ? googleUserInfo.name() : googleUserInfo.email().split("@")[0];

                    newUser.setName(safeName);
                    newUser.setEmail(googleUserInfo.email());
                    newUser.setAuthProvider(AuthProvider.GOOGLE);
                    newUser.setGoogleId(googleUserInfo.googleId());
                    newUser.setVerified(true);
                    newUser.setActive(true);
                    newUser.setUnitValue(java.math.BigDecimal.TEN); // That needs to change in the future.

                    return userRepository.save(newUser);
                });
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public AuthResponse refresh(RefreshTokenRequest request){
        User user = refreshTokenService.consumeToken(request.refreshToken());

        String newRefreshToken = refreshTokenService.generateToken(user);
        String newAccessToken = jwtService.generateToken(new UserDetailsImpl(user));

        return new AuthResponse(
                newRefreshToken,
                newAccessToken,
                UserResponse.fromEntity(user)
        );
    }

    @Transactional
    public void logout(LogoutRequest request){
        refreshTokenService.revokeToken(request.refreshToken());
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public void verifyEmail(Long userId, EmailVerificationRequest request){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if(user.isVerified()){
            throw new DataConflictException("User is already verified.");
        }

        otpTokenService.verifyOtp(user, request.otp(), OtpPurpose.EMAIL_VERIFICATION);

        user.setVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public void resendEmailVerification(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if(user.isVerified()){
            throw new DataConflictException("User is already verified.");
        }

        String otp = otpTokenService.createOtp(
                user,
                OtpPurpose.EMAIL_VERIFICATION,
                LocalDateTime.now().plusHours(24)
        );

        emailService.sendVerificationEmail(user.getEmail(), user.getName(), otp, false);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request){
        userRepository.findByEmail(request.email())
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
        // Prevents email enumeration. By mimicking the error for a missing OTP, attackers cannot distinguish between an unregistered email and an inactive reset request.
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("No verification code was requested."));

        otpTokenService.verifyOtp(user, request.otp(), OtpPurpose.PASSWORD_RESET);

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessRuleException("New password cannot be the same as your current password.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        emailService.sendPasswordChangeNotice(user.getEmail(), user.getName());
    }
}