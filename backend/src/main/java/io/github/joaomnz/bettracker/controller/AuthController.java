package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.dto.auth.*;
import io.github.joaomnz.bettracker.dto.user.ForgotPasswordRequest;
import io.github.joaomnz.bettracker.dto.user.ResetPasswordRequest;
import io.github.joaomnz.bettracker.security.UserDetailsImpl;
import io.github.joaomnz.bettracker.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signIn(@Valid @RequestBody SignInRequest request){
        AuthResponse response = authService.signIn(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request){
        AuthResponse response = authService.refresh(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request){
        authService.logout(request);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email-verification")
    public ResponseEntity<Void> verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
        authService.verifyEmail(userDetails.getUser().getId(), request);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email-verification/resend")
    public ResponseEntity<Void> resendEmailVerification(@AuthenticationPrincipal UserDetailsImpl userDetails){
        authService.resendEmailVerification(userDetails.getUser().getId());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request){
        authService.forgotPassword(request);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request){
        authService.resetPassword(request);

        return ResponseEntity.noContent().build();
    }
}