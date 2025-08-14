package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.dto.auth.LoginResponseDTO;
import io.github.joaomnz.bettracker.dto.auth.RegisterRequestDTO;
import io.github.joaomnz.bettracker.dto.bettor.BettorResponseDTO;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.service.BettorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RequestMapping("/api/v1")
@RestController
public class BettorController {
    @Autowired
    BettorService bettorService;

    @PostMapping("/auth/register")
    public ResponseEntity<LoginResponseDTO> createUser(@RequestBody RegisterRequestDTO newUser){
        Bettor createdBettor = bettorService.createBettor(newUser);
        String token = bettorService.generateTokenForBettor(createdBettor);

        LoginResponseDTO loginResponseDTO = new LoginResponseDTO(
                token,
                createdBettor.getId(),
                createdBettor.getName(),
                createdBettor.getEmail(),
                createdBettor.getType()
        );

        URI locationOfNewBettor = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdBettor.getId()).toUri();

        return ResponseEntity.created(locationOfNewBettor).body(loginResponseDTO);
    }

    @GetMapping("/users/me")
    public ResponseEntity<BettorResponseDTO> getCurrentUser(Authentication authentication) {
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
