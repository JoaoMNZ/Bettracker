package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.auth.RegisterRequestDTO;
import io.github.joaomnz.bettracker.exceptions.EmailAlreadyExistsException;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.repository.BettorRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class BettorService {
    private final BettorRepository bettorRepository;
    private final PasswordEncoder passwordEncoder;

    public BettorService(BettorRepository bettorRepository,
                         PasswordEncoder passwordEncoder) {
        this.bettorRepository = bettorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Bettor register(RegisterRequestDTO registerRequest){
        bettorRepository.findByEmail(registerRequest.email())
                .ifPresent(existing -> {
                    throw new EmailAlreadyExistsException("Email '" + registerRequest.email() + "' is already in use.");
                });
        Bettor bettor = new Bettor();
        bettor.setName(registerRequest.name());
        bettor.setEmail(registerRequest.email());
        bettor.setPassword(passwordEncoder.encode(registerRequest.password()));
        return bettorRepository.save(bettor);
    }

    public Bettor findByEmail(String email) {
        return bettorRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Bettor not found with email: " + email));
    }
}
