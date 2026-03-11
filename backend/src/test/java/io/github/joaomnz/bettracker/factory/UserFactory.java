package io.github.joaomnz.bettracker.factory;

import io.github.joaomnz.bettracker.dto.SignInRequest;
import io.github.joaomnz.bettracker.dto.SignUpRequest;
import io.github.joaomnz.bettracker.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

public final class UserFactory {
    private static final String DEFAULT_NAME = "Test User";
    private static final String DEFAULT_EMAIL = "test@example.com";
    public static final String DEFAULT_PASSWORD = "Pass123!";
    private static final BigDecimal DEFAULT_UNIT_VALUE = BigDecimal.TEN;
    private static final PasswordEncoder PASSWORD_ENCODER  = new BCryptPasswordEncoder(12);

    private UserFactory() {}

    public static User createUser() {
        return new User(
                DEFAULT_NAME,
                DEFAULT_EMAIL,
                PASSWORD_ENCODER .encode(DEFAULT_PASSWORD),
                DEFAULT_UNIT_VALUE
        );
    }

    public static User createUser(String email) {
        return new User(
                DEFAULT_NAME,
                email,
                PASSWORD_ENCODER .encode(DEFAULT_PASSWORD),
                DEFAULT_UNIT_VALUE
        );
    }

    public static SignUpRequest createSignUpRequest() {
        return new SignUpRequest(
                DEFAULT_NAME,
                DEFAULT_EMAIL,
                DEFAULT_PASSWORD,
                DEFAULT_UNIT_VALUE
        );
    }

    public static SignUpRequest createSignUpRequest(String email) {
        return new SignUpRequest(
                DEFAULT_NAME,
                email,
                DEFAULT_PASSWORD,
                DEFAULT_UNIT_VALUE
        );
    }

    public static SignUpRequest createSignUpRequest(String name, String email, String password, BigDecimal unitValue) {
        return new SignUpRequest(
                name,
                email,
                password,
                unitValue
        );
    }

    public static SignInRequest createSignInRequest(String email, String password){
        return new SignInRequest(email, password);
    }
}