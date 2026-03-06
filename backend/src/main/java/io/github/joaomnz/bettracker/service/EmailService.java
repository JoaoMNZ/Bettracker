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

    @Value("${app.email.sender}")
    private String senderEmail;

    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Async
    public void sendVerificationEmail(String to, String otp) {
        String subject = "BetTracker - Verify your email";
        String text = String.format(
                "Welcome to BetTracker!\n\n" +
                        "Your verification code is: %s\n\n" +
                        "This code will expire in 24 hours. If you did not request this, please ignore this email.",
                otp
        );

        sendEmail(to, subject, text);
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
