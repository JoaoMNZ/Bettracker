package io.github.joaomnz.bettracker.factory;

import io.github.joaomnz.bettracker.enums.OtpPurpose;
import io.github.joaomnz.bettracker.model.OtpToken;
import io.github.joaomnz.bettracker.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

public final class OtpTokenFactory {
    private OtpTokenFactory(){}

    public static final String DEFAULT_CODE = "123456";
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(12);

    public static OtpToken createEmailVerification(User user){
        return new OtpToken(
                user,
                PASSWORD_ENCODER.encode(DEFAULT_CODE),
                OtpPurpose.EMAIL_VERIFICATION,
                LocalDateTime.now().plusHours(24)
        );
    }

    public static OtpToken createEmailVerification(User user, LocalDateTime expiresAt) {
        return new OtpToken(
                user,
                PASSWORD_ENCODER.encode(DEFAULT_CODE),
                OtpPurpose.EMAIL_VERIFICATION,
                expiresAt
        );
    }
}
