package io.github.joaomnz.bettracker.security;

import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.repository.BettorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class BettorDetailsService implements UserDetailsService {
    @Autowired
    private BettorRepository bettorRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Bettor bettor = bettorRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Bettor not found with email: " + username));
        return new BettorDetails(bettor);
    }
}
