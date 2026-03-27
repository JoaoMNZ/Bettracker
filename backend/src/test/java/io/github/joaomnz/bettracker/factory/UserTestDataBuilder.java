package io.github.joaomnz.bettracker.factory;

import io.github.joaomnz.bettracker.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

public class UserTestDataBuilder {
    private String name = "Default Name";
    private String email = "default@email.com";
    private String password = "Pass123!";
    private String googleId;
    private int failedLoginAttempts = 0;
    private LocalDateTime lockoutEnd;
    private boolean active = true;
    private boolean verified = false;
    private final PasswordEncoder ENCODER = new BCryptPasswordEncoder(12);

    public UserTestDataBuilder withName(String name){
        this.name = name;
        return this;
    }

    public UserTestDataBuilder withEmail(String email){
        this.email = email;
        return this;
    }

    public UserTestDataBuilder withPassword(String password){
        this.password = password;
        return this;
    }

    public UserTestDataBuilder withGoogleId(String googleId){
        this.googleId = googleId;
        return this;
    }

    public UserTestDataBuilder withFailedLoginAttempts(int failedLoginAttempts){
        this.failedLoginAttempts = failedLoginAttempts;
        return this;
    }

    public UserTestDataBuilder withLockoutEnd(LocalDateTime lockoutEnd){
        this.lockoutEnd = lockoutEnd;
        return this;
    }

    public UserTestDataBuilder withActive(boolean active){
        this.active = active;
        return this;
    }

    public UserTestDataBuilder withVerified(boolean verified){
        this.verified = verified;
        return this;
    }

    public User build(){
        return User.builder()
                .name(name)
                .email(email)
                .password(ENCODER.encode(password))
                .googleId(googleId)
                .failedLoginAttempts(failedLoginAttempts)
                .lockoutEnd(lockoutEnd)
                .active(active)
                .verified(verified)
                .build();
    }
}