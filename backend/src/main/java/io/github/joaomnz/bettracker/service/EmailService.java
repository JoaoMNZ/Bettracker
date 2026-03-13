package io.github.joaomnz.bettracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

    @Async
    public void sendVerificationEmail(String to, String name, String otp, boolean isNewUser) {
        String subject = "BetTracker - Email Verification";

        String greetingLine = isNewUser
                ? "Welcome to BetTracker! Your email verification code is:"
                : "Your email verification code is:";

        String text = String.format(
                """
                Hi %s,
                
                %s
                
                %s
                
                This code will expire in 24 hours. If you did not request this, please ignore this email.
                
                Best regards,
                The BetTracker Team
                """,
                name, greetingLine, otp
        );

        sendEmail(to, subject, text);
    }

    @Async
    public void sendPasswordChangeNotice(String to, String name) {
        String subject = "Security Alert: Your Password Has Been Changed";
        String text = String.format(
                """
                Hi %s,
                
                This email is to confirm that the password for your BetTracker account has been successfully changed.
                
                If you did not authorize this action, please contact our support team immediately so we can secure your account.
                
                Best regards,
                The BetTracker Team
                """,
                name
        );

        sendEmail(to, subject, text);
    }

    @Async
    public void sendDeactivationEmail(String to, String name){
        String subject = "BetTracker - Account Deactivated";
        String text = String.format(
                """
                Hi %s,
                
                This email is to confirm that your BetTracker account has been successfully deactivated.
                
                If you did not authorize this action, please contact our support team immediately so we can secure your account.
                
                Best regards,
                The BetTracker Team
                """,
                name
        );

        sendEmail(to, subject, text);
    }

    @Async
    public void sendPasswordResetEmail(String to, String name, String otp) {
        String subject = "BetTracker - Password Reset Code";
        String text = String.format(
                """
                Hi %s,
                
                We received a request to reset the password for your BetTracker account.
                
                Your password reset code is: %s
                
                This code will expire in 15 minutes.
                
                If you did not request a password reset, you can safely ignore this email. Your password will not be changed.
                
                Best regards,
                The BetTracker Team
                """,
                name, otp
        );

        sendEmail(to, subject, text);
    }

    @Async
    public void sendEmailChangeVerificationEmail(String to, String name, String otp) {
        String subject = "BetTracker - Verify Your New Email Address";
        String text = String.format(
                """
                Hi %s,
                
                We received a request to change the email address associated with your BetTracker account to this one.
                
                Your verification code is: %s
                
                This code will expire in 15 minutes.
                
                If you did not request this change, please ignore this email. Your account email will remain unchanged.
                
                Best regards,
                The BetTracker Team
                """,
                name, otp
        );

        sendEmail(to, subject, text);
    }

    @Async
    public void sendEmailChangeNotice(String oldEmail, String name) {
        String subject = "Security Alert: Your BetTracker Email Was Changed";
        String text = String.format(
                """
                Hi %s,
                
                This is an automated notification that the email address associated with your BetTracker account was recently changed.
                
                If you made this change, no further action is required.
                
                If you did NOT authorize this change, please contact our support team immediately.
                
                Best regards,
                The BetTracker Team
                """,
                name
        );
        sendEmail(oldEmail, subject, text);
    }

    private void sendEmail(String to, String subject, String text){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try{
            javaMailSender.send(message);
            log.info("Email sent successfully to {}", to);
        }catch(Exception exception) {
            log.error("Failed to send email to {}: {}", to, exception.getMessage());
        }
    }
}
