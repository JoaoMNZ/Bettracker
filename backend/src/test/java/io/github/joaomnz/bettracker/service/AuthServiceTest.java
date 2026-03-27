package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.auth.*;
import io.github.joaomnz.bettracker.enums.OtpPurpose;
import io.github.joaomnz.bettracker.exception.BusinessRuleException;
import io.github.joaomnz.bettracker.exception.DataConflictException;
import io.github.joaomnz.bettracker.factory.UserTestDataBuilder;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.repository.UserRepository;
import io.github.joaomnz.bettracker.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtProvider jwtProvider;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private OtpTokenService otpTokenService;
    @Mock private EmailService emailService;
    @Mock private GoogleAuthService googleAuthService;
    @Spy private Clock clock = Clock.fixed(Instant.parse("2026-03-26T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("Sign Up")
    class SignUpTests {
        @Captor private ArgumentCaptor<User> userCaptor;

        @Test
        void shouldSaveUserAndReturnTokensWhenSignUpDataIsValid(){
            String name = "Test User";
            String email = "  TEST@Email.com  ";
            String expectedNormalizedEmail = "test@email.com";
            String password = "Pass123!";
            SignUpRequest request = new SignUpRequest(name, email, password);

            User mockSavedUser = new UserTestDataBuilder()
                    .withName(name)
                    .withEmail(expectedNormalizedEmail)
                    .build();

            when(userRepository.existsByEmail(expectedNormalizedEmail)).thenReturn(false);
            when(passwordEncoder.encode(password)).thenReturn("encoded-pass");
            when(userRepository.save(any(User.class))).thenReturn(mockSavedUser);

            when(otpTokenService.createOtp(same(mockSavedUser), eq(OtpPurpose.EMAIL_VERIFICATION), any(LocalDateTime.class))).thenReturn("123456");
            when(refreshTokenService.generateToken(same(mockSavedUser))).thenReturn("mock-refresh");
            when(jwtProvider.generateToken(argThat(ud -> ud.getUsername().equals(expectedNormalizedEmail)))).thenReturn("mock-jwt");

            AuthResponse response = authService.signUp(request);

            verify(userRepository).save(userCaptor.capture());
            User capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getName()).isEqualTo(name);
            assertThat(capturedUser.getEmail()).isEqualTo(expectedNormalizedEmail);
            assertThat(capturedUser.getPassword()).isEqualTo("encoded-pass");

            assertThat(response).isNotNull();
            assertThat(response.refreshToken()).isEqualTo("mock-refresh");
            assertThat(response.accessToken()).isEqualTo("mock-jwt");
            assertThat(response.user().name()).isEqualTo(name);
            assertThat(response.user().email()).isEqualTo(expectedNormalizedEmail);

            verify(emailService).sendEmailVerificationCode(expectedNormalizedEmail, name, "123456");
        }

        @Test
        void shouldThrowDataConflictExceptionWhenEmailIsAlreadyRegistered(){
            SignUpRequest request = new SignUpRequest("Test User", "test@email.com", "Pass123!");
            when(userRepository.existsByEmail("test@email.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.signUp(request))
                    .isInstanceOf(DataConflictException.class)
                    .hasMessage("The email address is already registered.");

            verify(userRepository, never()).save(any(User.class));
            verifyNoInteractions(passwordEncoder, otpTokenService, emailService, refreshTokenService, jwtProvider);
        }
    }

    @Nested
    @DisplayName("Sign In")
    class SignInTests {
        @Test
        void shouldReturnTokensAndResetAttemptsWhenCredentialsAreValid(){
            String email = "  TEST@Email.com  ";
            String expectedNormalizedEmail = "test@email.com";
            String password = "Pass123!";

            SignInRequest request = new SignInRequest(email, password);

            User mockUser = new UserTestDataBuilder()
                    .withEmail(expectedNormalizedEmail)
                    .withPassword(password)
                    .withFailedLoginAttempts(3)
                    .build();

            when(userRepository.findByEmail(expectedNormalizedEmail)).thenReturn(Optional.of(mockUser));
            when(refreshTokenService.generateToken(same(mockUser))).thenReturn("mock-refresh");
            when(jwtProvider.generateToken(argThat(ud -> ud.getUsername().equals(expectedNormalizedEmail)))).thenReturn("mock-jwt");

            AuthResponse response = authService.signIn(request);

            assertThat(response).isNotNull();
            assertThat(response.refreshToken()).isEqualTo("mock-refresh");
            assertThat(response.accessToken()).isEqualTo("mock-jwt");
            assertThat(response.user().email()).isEqualTo(expectedNormalizedEmail);

            assertThat(mockUser.getFailedLoginAttempts()).isEqualTo(0);
            assertThat(mockUser.getLockoutEnd()).isNull();

            verify(authenticationManager).authenticate(argThat(token ->
                    Objects.equals(token.getPrincipal(), expectedNormalizedEmail) && Objects.equals(token.getCredentials(), password)
            ));
        }

        @Test
        void shouldThrowExceptionWhenEmailIsNotFound(){
            SignInRequest request = new SignInRequest("test@email.com", "Pass123!");

            when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.signIn(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Invalid credentials.");

            verifyNoInteractions(authenticationManager, refreshTokenService, jwtProvider);
        }

        @Test
        void shouldThrowBusinessRuleExceptionWhenPasswordIsNull(){
            SignInRequest request = new SignInRequest("test@email.com", "Pass123!");

            User mockUser = new UserTestDataBuilder()
                    .withPassword(null)
                    .build();

            when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(mockUser));

            assertThatThrownBy(() -> authService.signIn(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Invalid credentials.");

            verifyNoInteractions(authenticationManager, refreshTokenService, jwtProvider);
        }

        @Test
        void shouldThrowLockedExceptionWhenAccountIsAlreadyLocked(){
            SignInRequest request = new SignInRequest("test@email.com", "Pass123!");

            User mockUser = new UserTestDataBuilder()
                    .withLockoutEnd(LocalDateTime.now(clock).plusMinutes(15))
                    .build();

            when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(mockUser));

            assertThatThrownBy(() -> authService.signIn(request))
                    .isInstanceOf(LockedException.class)
                    .hasMessageContaining("Account is locked due to too many failed attempts. Please try again in 15 minute(s).");

            verifyNoInteractions(authenticationManager, refreshTokenService, jwtProvider);
        }

        @Test
        void shouldAllowLoginAndResetAttemptsWhenLockoutHasExpired() {
            String email = "test@email.com";
            String password = "Pass123!";
            SignInRequest request = new SignInRequest(email, password);

            User mockUser = new UserTestDataBuilder()
                    .withEmail(email)
                    .withPassword(password)
                    .withFailedLoginAttempts(3)
                    .withLockoutEnd(LocalDateTime.now(clock).minusMinutes(1))
                    .build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
            when(refreshTokenService.generateToken(same(mockUser))).thenReturn("mock-refresh");
            when(jwtProvider.generateToken(argThat(ud -> ud.getUsername().equals(email)))).thenReturn("mock-jwt");

            AuthResponse response = authService.signIn(request);

            assertThat(response).isNotNull();

            assertThat(mockUser.getFailedLoginAttempts()).isEqualTo(0);
            assertThat(mockUser.getLockoutEnd()).isNull();
        }

        @Test
        void shouldThrowDisabledExceptionWhenAccountIsDeactivated(){
            SignInRequest request = new SignInRequest("test@email.com", "Pass123!");

            User mockUser = new UserTestDataBuilder()
                    .withActive(false)
                    .build();

            when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(mockUser));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(DisabledException.class);

            assertThatThrownBy(() -> authService.signIn(request))
                    .isInstanceOf(DisabledException.class);

            verifyNoInteractions(refreshTokenService, jwtProvider);
        }

        @Test
        void shouldIncrementFailedAttemptsWhenPasswordIsIncorrect(){
            SignInRequest request = new SignInRequest("test@email.com", "wrong-password");

            User mockUser = new UserTestDataBuilder()
                    .withPassword("Pass123!")
                    .build();

            when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(mockUser));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(BadCredentialsException.class);

            assertThatThrownBy(() -> authService.signIn(request))
                    .isInstanceOf(BadCredentialsException.class);

            assertThat(mockUser.getFailedLoginAttempts()).isEqualTo(1);

            verifyNoInteractions(refreshTokenService, jwtProvider);
        }

        @Test
        void shouldLockAccountAndResetAttemptsOnFifthFailedLogin(){
            SignInRequest request = new SignInRequest("test@email.com", "wrong-password");

            User mockUser = new UserTestDataBuilder()
                    .withPassword("Pass123!")
                    .withFailedLoginAttempts(4)
                    .build();

            when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(mockUser));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(BadCredentialsException.class);

            assertThatThrownBy(() -> authService.signIn(request))
                    .isInstanceOf(LockedException.class)
                    .hasMessage("Account locked due to too many failed attempts. Please try again in 15 minutes.");

            assertThat(mockUser.getFailedLoginAttempts()).isEqualTo(0);
            assertThat(mockUser.getLockoutEnd()).isNotNull();

            verifyNoInteractions(refreshTokenService, jwtProvider);
        }
    }

    @Nested
    @DisplayName("Google OAuth Authentication")
    class GoogleOAuthTests {
        @Captor private ArgumentCaptor<User> userCaptor;

        @Test
        void shouldAuthenticateReturningGoogleUserAndResetLockout(){
            String name = "Test User";
            String email = "test@email.com";
            String googleId = "google-id";

            GoogleLoginRequest request = new GoogleLoginRequest("google-token");
            GoogleUserInfo mockGoogleUserInfo = new GoogleUserInfo(email, name, googleId);

            User mockUser = new UserTestDataBuilder()
                    .withName(name)
                    .withEmail(email)
                    .withGoogleId(googleId)
                    .withFailedLoginAttempts(3)
                    .withLockoutEnd(LocalDateTime.now(clock).plusMinutes(15))
                    .build();

            when(googleAuthService.verifyToken(request.token())).thenReturn(mockGoogleUserInfo);
            when(userRepository.findByGoogleId(googleId)).thenReturn(Optional.of(mockUser));
            when(refreshTokenService.generateToken(same(mockUser))).thenReturn("mock-refresh");
            when(jwtProvider.generateToken(argThat(ud -> ud.getUsername().equals(email)))).thenReturn("mock-jwt");

            AuthResponse response = authService.authenticateWithGoogle(request);

            verify(userRepository, never()).findByEmail(any());

            assertThat(mockUser.getFailedLoginAttempts()).isEqualTo(0);
            assertThat(mockUser.getLockoutEnd()).isNull();
            verify(userRepository).save(same(mockUser));

            assertThat(response).isNotNull();
            assertThat(response.refreshToken()).isEqualTo("mock-refresh");
            assertThat(response.accessToken()).isEqualTo("mock-jwt");
            assertThat(response.user().name()).isEqualTo(name);
            assertThat(response.user().email()).isEqualTo(email);

            verifyNoInteractions(emailService);
        }

        @Test
        void shouldCreateNewUserWhenGoogleIdAndEmailAreNew(){
            String name = "Test User";
            String email = "  TEST@Email.com  ";
            String expectedNormalizedEmail = "test@email.com";
            String googleId = "google-id";

            GoogleLoginRequest request = new GoogleLoginRequest("google-token");
            GoogleUserInfo mockGoogleUserInfo = new GoogleUserInfo(email, name, googleId);

            User mockSavedUser = new UserTestDataBuilder()
                    .withName(name)
                    .withEmail(expectedNormalizedEmail)
                    .withGoogleId(googleId)
                    .withVerified(true)
                    .build();

            when(googleAuthService.verifyToken(request.token())).thenReturn(mockGoogleUserInfo);
            when(userRepository.findByGoogleId(googleId)).thenReturn(Optional.empty());
            when(userRepository.findByEmail(expectedNormalizedEmail)).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenReturn(mockSavedUser);
            when(refreshTokenService.generateToken(same(mockSavedUser))).thenReturn("mock-refresh");
            when(jwtProvider.generateToken(argThat(ud -> ud.getUsername().equals(expectedNormalizedEmail)))).thenReturn("mock-jwt");

            authService.authenticateWithGoogle(request);

            verify(userRepository).findByEmail(expectedNormalizedEmail);

            verify(userRepository, times(2)).save(userCaptor.capture());
            User capturedUser = userCaptor.getAllValues().getFirst();
            assertThat(capturedUser.getName()).isEqualTo(name);
            assertThat(capturedUser.getEmail()).isEqualTo(expectedNormalizedEmail);
            assertThat(capturedUser.getGoogleId()).isEqualTo(googleId);
            assertThat(capturedUser.isVerified()).isTrue();
            assertThat(capturedUser.getPassword()).isNull();

            verify(emailService).sendWelcome(mockSavedUser.getEmail(), mockSavedUser.getName());
        }

        @Test
        void shouldLinkGoogleAccountAndSendWelcomeWhenEmailExistsAndUnverified(){
            String name = "Test User";
            String email = "test@email.com";
            String googleId = "google-id";

            GoogleLoginRequest request = new GoogleLoginRequest("google-token");
            GoogleUserInfo mockGoogleUserInfo = new GoogleUserInfo(email, name, googleId);

            User mockExistingUser = new UserTestDataBuilder()
                    .withName(name)
                    .withEmail(email)
                    .withGoogleId(null)
                    .withVerified(false)
                    .build();

            when(googleAuthService.verifyToken(request.token())).thenReturn(mockGoogleUserInfo);
            when(userRepository.findByGoogleId(googleId)).thenReturn(Optional.empty());
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockExistingUser));
            when(userRepository.save(same(mockExistingUser))).thenReturn(mockExistingUser);
            when(refreshTokenService.generateToken(same(mockExistingUser))).thenReturn("mock-refresh");
            when(jwtProvider.generateToken(argThat(ud -> ud.getUsername().equals(email)))).thenReturn("mock-jwt");

            authService.authenticateWithGoogle(request);

            verify(userRepository).findByEmail(email);

            assertThat(mockExistingUser.getGoogleId()).isEqualTo(googleId);
            assertThat(mockExistingUser.isVerified()).isTrue();

            verify(userRepository, times(2)).save(same(mockExistingUser));

            verify(emailService).sendWelcome(email, name);
        }

        @Test
        void shouldLinkGoogleAccountWithoutSendingWelcomeWhenUserAlreadyVerified(){
            String email = "test@email.com";
            String googleId = "google-id";

            GoogleLoginRequest request = new GoogleLoginRequest("google-token");
            GoogleUserInfo mockGoogleUserInfo = new GoogleUserInfo(email, "Test User", googleId);

            User mockExistingUser = new UserTestDataBuilder()
                    .withEmail(email)
                    .withGoogleId(null)
                    .withVerified(true)
                    .build();

            when(googleAuthService.verifyToken(request.token())).thenReturn(mockGoogleUserInfo);
            when(userRepository.findByGoogleId(googleId)).thenReturn(Optional.empty());
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockExistingUser));
            when(userRepository.save(same(mockExistingUser))).thenReturn(mockExistingUser);
            when(refreshTokenService.generateToken(same(mockExistingUser))).thenReturn("mock-refresh");
            when(jwtProvider.generateToken(argThat(ud -> ud.getUsername().equals(email)))).thenReturn("mock-jwt");

            authService.authenticateWithGoogle(request);

            verify(userRepository).findByEmail(email);

            verify(emailService, never()).sendWelcome(any(), any());
        }

        @Test
        void shouldThrowDisabledExceptionAndNotResetLockoutWhenAccountIsInactive(){
            String email = "test@email.com";
            String googleId = "google-id";

            GoogleLoginRequest request = new GoogleLoginRequest("google-token");
            GoogleUserInfo mockGoogleUserInfo = new GoogleUserInfo(email, "Test User", googleId);

            User mockUser = new UserTestDataBuilder()
                    .withEmail(email)
                    .withGoogleId(googleId)
                    .withFailedLoginAttempts(3)
                    .withLockoutEnd(LocalDateTime.now(clock).plusMinutes(15))
                    .withActive(false)
                    .build();

            when(googleAuthService.verifyToken(request.token())).thenReturn(mockGoogleUserInfo);
            when(userRepository.findByGoogleId(googleId)).thenReturn(Optional.of(mockUser));

            assertThatThrownBy(() -> authService.authenticateWithGoogle(request))
                    .isInstanceOf(DisabledException.class)
                    .hasMessage("Your account is deactivated. Contact support.");

            verify(userRepository, never()).findByEmail(any());

            assertThat(mockUser.getFailedLoginAttempts()).isEqualTo(3);
            assertThat(mockUser.getLockoutEnd()).isNotNull();

            verify(userRepository,never()).save(any());
            verifyNoInteractions(refreshTokenService, jwtProvider, emailService);
        }

        @Test
        void shouldThrowDataConflictExceptionWhenEmailIsLinkedToDifferentGoogleId(){
            String email = "test@email.com";
            String googleId = "google-id";

            GoogleLoginRequest request = new GoogleLoginRequest("google-token");
            GoogleUserInfo mockGoogleUserInfo = new GoogleUserInfo(email, "Test User", googleId);

            User mockExistingUser = new UserTestDataBuilder()
                    .withEmail(email)
                    .withGoogleId("another-google-token")
                    .build();

            when(googleAuthService.verifyToken(request.token())).thenReturn(mockGoogleUserInfo);
            when(userRepository.findByGoogleId(googleId)).thenReturn(Optional.empty());
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockExistingUser));

            assertThatThrownBy(() -> authService.authenticateWithGoogle(request))
                    .isInstanceOf(DataConflictException.class)
                    .hasMessage("Email already linked to another Google identity.");

            verify(userRepository).findByEmail(email);

            verifyNoInteractions(refreshTokenService, jwtProvider, emailService);
            verify(userRepository, never()).save(any());
        }
    }
}