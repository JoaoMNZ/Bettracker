package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.annotation.AsyncRetryableEmail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Recover;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {
    private final JavaMailSender javaMailSender;
    private final String senderEmail;

    public EmailService(JavaMailSender javaMailSender, @Value("${app.email.sender}") String senderEmail) {
        this.javaMailSender = javaMailSender;
        this.senderEmail = senderEmail;
    }

    @AsyncRetryableEmail
    public void sendWelcomeEmail(String to, String name) {
        String content = """
        Welcome to BetTracker!
        
        Your account is verified and ready to go.
        """;

        sendEmail(to, "BetTracker - Welcome!", buildEmailBody(name, content));
    }

    @AsyncRetryableEmail
    public void sendVerificationEmail(String to, String name, String otp) {
        String content = String.format(
                """
                Your email verification code is: %s
                
                This code will expire in 24 hours. If you did not request this, please ignore this email.
                """,
                otp
        );

        sendEmail(to, "BetTracker - Email Verification", buildEmailBody(name, content));
    }

    @AsyncRetryableEmail
    public void sendPasswordResetEmail(String to, String name, String otp) {
        String content = String.format(
                """
                Your password reset code is: %s
                
                This code will expire in 15 minutes. If you did not request a reset, please ignore this email.
                """,
                otp
        );

        sendEmail(to, "BetTracker - Password Reset", buildEmailBody(name, content));
    }

    @AsyncRetryableEmail
    public void sendPasswordChangeNotice(String to, String name) {
        String content = """
        Your password has been successfully changed
        
        If you did not make this change, please contact support immediately.
        """;

        sendEmail(to, "BetTracker - Password Changed", buildEmailBody(name, content));
    }

    @AsyncRetryableEmail
    public void sendEmailChangeVerificationEmail(String to, String name, String otp) {
        String content = String.format(
                """
                Your verification code to change your email is: %s
                
                This code will expire in 15 minutes. If you did not request this change, please ignore this email.
                """,
                otp
        );

        sendEmail(to, "BetTracker - Verify New Email", buildEmailBody(name, content));
    }

    @AsyncRetryableEmail
    public void sendEmailChangeNotice(String oldEmail, String name) {
        String content = """
        Your account email address has been changed.
        
        If you did not authorize this change, please contact support immediately.
        """;
        sendEmail(oldEmail, "BetTracker - Email Changed", buildEmailBody(name, content));
    }

    @AsyncRetryableEmail
    public void sendDeactivationEmail(String to, String name){
        String content = """
        Your account has been successfully deactivated.
        
        If you did not request this, please contact support immediately.
        """;

        sendEmail(to, "BetTracker - Account Deactivated", buildEmailBody(name, content));
    }

    private void sendEmail(String to, String subject, String text){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        javaMailSender.send(message);
        log.info("Email sent successfully to {}", to);
    }

    @Recover
    public void recoverEmailFailure(MailException exception, String to) {
        log.error("Failed to send email to {}: {}", to, exception.getMessage(), exception);
    }

    private String buildEmailBody(String name, String content) {
        return String.format(
                """
                Hi %s,
                
                %s
                
                Best regards,
                The BetTracker Team
                """,
                name, content
        );
    }
}
