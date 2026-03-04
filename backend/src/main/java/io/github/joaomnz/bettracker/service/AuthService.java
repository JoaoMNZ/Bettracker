package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.AuthResponse;
import io.github.joaomnz.bettracker.dto.SignInRequest;
import io.github.joaomnz.bettracker.dto.SignUpRequest;
import io.github.joaomnz.bettracker.dto.UserResponse;
import io.github.joaomnz.bettracker.exception.DataConflictException;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.repository.UserRepository;
import io.github.joaomnz.bettracker.security.TokenService;
import io.github.joaomnz.bettracker.security.UserDetailsImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse signUp(SignUpRequest request){
        if(userRepository.existsByEmail(request.email())){
            throw new DataConflictException("The email address is already registered.");
        }

        User savedUser = userRepository.save(
                new User(
                    request.name(),
                    request.email(),
                    passwordEncoder.encode(request.password()),
                    request.unitValue()
                )
        );

        return new AuthResponse(
                tokenService.generateToken(new UserDetailsImpl(savedUser)),
                UserResponse.fromEntity(savedUser)
        );
    }

    public AuthResponse signIn(SignInRequest request){
        UsernamePasswordAuthenticationToken authToken  = new UsernamePasswordAuthenticationToken(request.email(), request.password());
        Authentication authentication = authenticationManager.authenticate(authToken);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return new AuthResponse(
                tokenService.generateToken(userDetails),
                UserResponse.fromEntity(userDetails.getUser())
        );
    }
}
