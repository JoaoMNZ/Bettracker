package io.github.joaomnz.bettracker.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtTokenService {
    @Value("${jwt.secret.key}")
    private String secretKey;

    @Value("${jwt.issuer}")
    private String issuer;

    public String generateToken(BettorDetails bettorDetails){
        try {
            Algorithm algorithm = Algorithm.HMAC256(secretKey);
            return JWT.create()
                    .withIssuer(issuer)
                    .withSubject(bettorDetails.getUsername())
                    .withClaim("role", bettorDetails.getAuthorities()
                            .stream()
                            .map(GrantedAuthority::getAuthority)
                            .toList())
                    .withIssuedAt(creationDate())
                    .withExpiresAt(expirationDate())
                    .sign(algorithm);
        }catch (JWTCreationException jwtCreationException){
            throw new JWTCreationException("Error generating token.", jwtCreationException);
        }
    }

    public String getSubjectFromToken(String token){
        try {
            Algorithm algorithm = Algorithm.HMAC256(secretKey);
            return JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build()
                    .verify(token)
                    .getSubject();
        }catch(JWTVerificationException jwtVerificationException) {
            throw new JWTVerificationException("Invalid or expired token.", jwtVerificationException);
        }
    }

    private Instant creationDate(){
        return Instant.now();
    }

    private Instant expirationDate(){
        return Instant.now().plus(4, ChronoUnit.HOURS);
    }
}
