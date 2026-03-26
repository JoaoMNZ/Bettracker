package io.github.joaomnz.bettracker.factory;

import io.github.joaomnz.bettracker.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

public class UserTestDataBuilder {
    private String name = "Default Name";
    private String email = "default@email.com";
    private String password = "Pass123!";
    private int failedLoginAttempts = 0;
    private LocalDateTime lockoutEnd = null;
    private final PasswordEncoder ENCODER  = new BCryptPasswordEncoder(12);

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

    public UserTestDataBuilder withFailedLoginAttempts(int failedLoginAttempts){
        this.failedLoginAttempts = failedLoginAttempts;
        return this;
    }

    public UserTestDataBuilder withLockoutEnd(LocalDateTime lockoutEnd){
        this.lockoutEnd = lockoutEnd;
        return this;
    }

    public User build(){
        return User.builder()
                .name(name)
                .email(email)
                .password(ENCODER.encode(password))
                .failedLoginAttempts(failedLoginAttempts)
                .lockoutEnd(lockoutEnd)
                .build();
    }
}