package io.github.joaomnz.bettracker.controller;

import io.github.joaomnz.bettracker.dto.DeactivateAccountRequest;
import io.github.joaomnz.bettracker.security.UserDetailsImpl;
import io.github.joaomnz.bettracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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