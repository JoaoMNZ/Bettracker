package io.github.joaomnz.bettracker.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    @Autowired
    private BettorAuthenticationFilter bettorAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception{
        return httpSecurity
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/tipsters").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/sports").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/bookmakers").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/sports/{sportId}/competitions").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/bookmakers/{bookmakerId}/transactions").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/bets").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/bets/{id}").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/bets/{id}").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/bets/{id}").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/bets").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tipsters/{id}").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tipsters").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/tipsters/{id}").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/tipsters/{id}").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookmakers").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookmakers/{id}").authenticated()
                        .anyRequest().denyAll()
                )
                .addFilterBefore(bettorAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception{
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
