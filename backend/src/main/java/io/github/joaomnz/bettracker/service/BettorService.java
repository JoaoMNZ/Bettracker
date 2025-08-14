package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.auth.RegisterRequestDTO;
import io.github.joaomnz.bettracker.exceptions.EmailAlreadyExistsException;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.repository.BettorRepository;
import io.github.joaomnz.bettracker.security.BettorDetails;
import io.github.joaomnz.bettracker.security.JwtTokenService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class BettorService {
    private final BettorRepository bettorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public BettorService(BettorRepository bettorRepository, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService) {
        this.bettorRepository = bettorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public Bettor createBettor(RegisterRequestDTO newUser){
        bettorRepository.findByEmail(newUser.email())
                .ifPresent(existing -> {
                    throw new EmailAlreadyExistsException("Email '" + newUser.email() + "' is already in use.");
                });
        Bettor bettor = new Bettor();
        bettor.setName(newUser.name());
        bettor.setEmail(newUser.email());
        bettor.setPassword(passwordEncoder.encode(newUser.password()));
        return bettorRepository.save(bettor);
    }

    public Bettor findByEmail(String email) {
        return bettorRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Bettor not found with email: " + email));
    }

    public String generateTokenForBettor(Bettor bettor) {
        return jwtTokenService.generateToken(new BettorDetails(bettor));
    }
}
