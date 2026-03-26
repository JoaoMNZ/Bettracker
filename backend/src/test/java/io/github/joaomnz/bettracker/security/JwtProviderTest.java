/*
package io.github.joaomnz.bettracker.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtProviderTest {
    private JwtProvider jwtProvider;
    private UserDetails testUser;

    @BeforeEach
    public void setUp() {
        jwtProvider = new JwtProvider("test-secret-key", "test-issuer");
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_FREE"));
        testUser = new User("test@hotmail.com", "Pass123!", authorities);
    }

    @Test
    @DisplayName("Should generate a valid JWT.")
    void shouldGenerateSuccessfully(){
        String token = jwtProvider.generateToken(testUser);
        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Should extract the subject (email) from a valid JWT.")
    void shouldExtractSubjectFromValidToken(){
        String token = jwtProvider.generateToken(testUser);
        String subject = jwtProvider.extractSubject(token);

        assertThat(subject).isNotNull();
        assertThat(subject).isEqualTo(testUser.getUsername());
    }

    @Test
    @DisplayName("Should return null when JWT is invalid.")
    void shouldReturnNullWhenTokenIsInvalid(){
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI.invalidpayload.invalidsignature";
        String subject = jwtProvider.extractSubject(token);

        assertThat(subject).isNull();
    }
}
*/