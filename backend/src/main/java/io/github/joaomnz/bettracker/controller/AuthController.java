package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.dto.AuthResponse;
import io.github.joaomnz.bettracker.dto.SignUpRequest;
import io.github.joaomnz.bettracker.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUp(
            @Valid @RequestBody SignUpRequest request,
            UriComponentsBuilder uriBuilder
    ){
        AuthResponse response = authService.signUp(request);

        return ResponseEntity.created(uriBuilder.path("/api/v1/users/me").build().toUri()).body(response);
    }
}