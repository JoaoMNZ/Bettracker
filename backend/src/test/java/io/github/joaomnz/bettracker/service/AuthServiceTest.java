package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.auth.AuthResponse;
import io.github.joaomnz.bettracker.dto.auth.SignUpRequest;
import io.github.joaomnz.bettracker.enums.OtpPurpose;
import io.github.joaomnz.bettracker.factory.UserTestDataBuilder;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.repository.UserRepository;
import io.github.joaomnz.bettracker.security.JwtProvider;
import io.github.joaomnz.bettracker.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtProvider jwtProvider;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private OtpTokenService otpTokenService;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    void shouldSignUpSuccessfully(){
        String name = "Test User";
        String email = "test@email.com";
        String password = "Pass123!";
        SignUpRequest request = new SignUpRequest(name, email, password);

        User mockSavedUser = new UserTestDataBuilder()
                .withName(name)
                .withEmail(email)
                .build();

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenReturn(mockSavedUser);

        when(otpTokenService.createOtp(eq(mockSavedUser), eq(OtpPurpose.EMAIL_VERIFICATION), any(LocalDateTime.class))).thenReturn("123456");
        when(refreshTokenService.generateToken(mockSavedUser)).thenReturn("mock-refresh");
        when(jwtProvider.generateToken(any(UserDetailsImpl.class))).thenReturn("mock-jwt");

        AuthResponse response = authService.signUp(request);

        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getName()).isEqualTo(name);
        assertThat(capturedUser.getEmail()).isEqualTo(email);
        assertThat(capturedUser.getPassword()).isEqualTo("encoded-pass");

        assertThat(response).isNotNull();
        assertThat(response.refreshToken()).isEqualTo("mock-refresh");
        assertThat(response.accessToken()).isEqualTo("mock-jwt");
        assertThat(response.user().name()).isEqualTo(name);
        assertThat(response.user().email()).isEqualTo(email);

        verify(emailService).sendEmailVerificationCode(email, name, "123456");
    }
}