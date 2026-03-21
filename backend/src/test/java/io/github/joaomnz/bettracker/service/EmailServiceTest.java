package io.github.joaomnz.bettracker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {
    @Mock
    private JavaMailSender javaMailSender;

    private EmailService emailService;

    private static final String SENDER_EMAIL = "noreply@bettracker.com";
    private static final String TARGET_EMAIL = "user@example.com";
    private static final String TARGET_NAME = "Test User";
    private static final String OTP = "123456";

    @BeforeEach
    void setUp() {
        this.emailService = new EmailService(javaMailSender, SENDER_EMAIL);
    }

    @Test
    @DisplayName("Should construct and send a formatted verification email for a new user.")
    void shouldSendVerificationEmailSuccessfully() {
        emailService.sendVerificationEmail(TARGET_EMAIL, TARGET_NAME, OTP, true);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage capturedMessage = messageCaptor.getValue();

        assertThat(capturedMessage.getFrom()).isEqualTo(SENDER_EMAIL);
        assertThat(capturedMessage.getTo()).containsExactly(TARGET_EMAIL);
        assertThat(capturedMessage.getSubject()).isEqualTo("BetTracker - Email Verification");
        assertThat(capturedMessage.getText()).contains("Welcome to BetTracker!");
        assertThat(capturedMessage.getText()).contains(OTP);
    }

    @Test
    @DisplayName("Should construct and send a deactivation confirmation email.")
    void shouldSendDeactivationEmailSuccessfully() {
        emailService.sendDeactivationEmail(TARGET_EMAIL, TARGET_NAME);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage capturedMessage = messageCaptor.getValue();

        assertThat(capturedMessage.getFrom()).isEqualTo(SENDER_EMAIL);
        assertThat(capturedMessage.getTo()).containsExactly(TARGET_EMAIL);
        assertThat(capturedMessage.getSubject()).isEqualTo("BetTracker - Account Deactivated");
        assertThat(capturedMessage.getText()).contains("successfully deactivated");
    }

    @Test
    @DisplayName("Should not throw an exception if the mail server fails.")
    void shouldHandleMailExceptionGracefully() {
        doThrow(new RuntimeException("Mail server is down!"))
                .when(javaMailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> emailService.sendVerificationEmail(TARGET_EMAIL, TARGET_NAME, OTP))
                .doesNotThrowAnyException();
    }
}
