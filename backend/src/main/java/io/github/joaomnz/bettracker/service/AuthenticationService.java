package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.auth.LoginRequestDTO;
import io.github.joaomnz.bettracker.dto.auth.LoginResponseDTO;
import io.github.joaomnz.bettracker.security.BettorDetails;
import io.github.joaomnz.bettracker.security.JwtTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private final JwtTokenService jwtTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(JwtTokenService jwtTokenService, AuthenticationManager authenticationManager) {
        this.jwtTokenService = jwtTokenService;
        this.authenticationManager = authenticationManager;
    }

    public LoginResponseDTO login(LoginRequestDTO loginRequest){
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password());
        Authentication authentication = authenticationManager.authenticate(authToken);
        BettorDetails bettorDetails = (BettorDetails) authentication.getPrincipal();
        String token = jwtTokenService.generateToken(bettorDetails);
        return new LoginResponseDTO(
                token,
                bettorDetails.getBettor().getId(),
                bettorDetails.getBettor().getName(),
                bettorDetails.getUsername(),
                bettorDetails.getBettor().getType()
        );
    }
}
