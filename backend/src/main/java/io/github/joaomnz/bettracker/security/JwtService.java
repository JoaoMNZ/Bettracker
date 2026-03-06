package io.github.joaomnz.bettracker.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class JwtService {
    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.token.issuer}")
    private String issuer;

    private Algorithm algorithm;

    @PostConstruct
    public void init() {
        this.algorithm = Algorithm.HMAC256(secret);
    }

    public String generateToken(UserDetails userDetails){
        try{
            List<String> roles = userDetails.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            return JWT.create()
                    .withIssuer(issuer)
                    .withSubject(userDetails.getUsername())
                    .withClaim("roles", roles)
                    .withIssuedAt(issuedAt())
                    .withExpiresAt(expiration())
                    .sign(algorithm);
        }catch(JWTCreationException exception){
            throw new RuntimeException("Error generating JWT.", exception);
        }
    }

    public String extractSubject(String token){
        try{
            return JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build()
                    .verify(token)
                    .getSubject();
        }catch(JWTVerificationException exception){
            // Returns null so the SecurityFilter can deny access without throwing a 500 error.
            return null;
        }
    }

    private Instant issuedAt(){
        return Instant.now();
    }

    private Instant expiration(){
        return Instant.now().plus(4, ChronoUnit.HOURS);
    }
}