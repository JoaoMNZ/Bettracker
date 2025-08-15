package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.dto.auth.LoginResponseDTO;
import io.github.joaomnz.bettracker.dto.auth.RegisterRequestDTO;
import io.github.joaomnz.bettracker.dto.bettor.BettorResponseDTO;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.security.BettorDetails;
import io.github.joaomnz.bettracker.security.JwtTokenService;
import io.github.joaomnz.bettracker.service.BettorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RequestMapping("/api/v1/users")
@RestController
public class BettorController {
    private final BettorService bettorService;
    private final JwtTokenService jwtTokenService;

    public BettorController(BettorService bettorService,
                            JwtTokenService jwtTokenService) {
        this.bettorService = bettorService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponseDTO> register(@Valid @RequestBody RegisterRequestDTO registerRequest){
        Bettor createdBettor = bettorService.register(registerRequest);
        String token = jwtTokenService.generateToken(new BettorDetails(createdBettor));

        LoginResponseDTO response = new LoginResponseDTO(
                token,
                createdBettor.getId(),
                createdBettor.getName(),
                createdBettor.getEmail(),
                createdBettor.getType()
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdBettor.getId()).toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<BettorResponseDTO> getMe(Authentication authentication) {
        Bettor bettor = bettorService.findByEmail(authentication.getName());
        BettorResponseDTO responseDTO = new BettorResponseDTO(
                bettor.getId(),
                bettor.getName(),
                bettor.getEmail(),
                bettor.getType(),
                bettor.getUnitValue(),
                bettor.getCreatedAt()
        );
        return ResponseEntity.ok(responseDTO);
    }
}
