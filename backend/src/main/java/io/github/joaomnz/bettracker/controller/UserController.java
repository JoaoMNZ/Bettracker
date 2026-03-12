package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.dto.user.DeactivateAccountRequest;
import io.github.joaomnz.bettracker.dto.user.UpdatePasswordRequest;
import io.github.joaomnz.bettracker.dto.user.UpdateProfileRequest;
import io.github.joaomnz.bettracker.dto.user.UserProfileResponse;
import io.github.joaomnz.bettracker.security.UserDetailsImpl;
import io.github.joaomnz.bettracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UserDetailsImpl userDetails){
        UserProfileResponse response = userService.getProfile(userDetails.getUser().getId());

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdateProfileRequest request
    ){
        UserProfileResponse response = userService.updateProfile(userDetails.getUser().getId(), request);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdatePasswordRequest request
    ){
        userService.updatePassword(userDetails.getUser().getId(), request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deactivateMyAccount(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody DeactivateAccountRequest request
    ){
        userService.deactivateAccount(userDetails.getUser().getId(), request);

        return ResponseEntity.noContent().build();
    }
}