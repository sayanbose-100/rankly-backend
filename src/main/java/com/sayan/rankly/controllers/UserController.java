package com.sayan.rankly.controllers;

import com.sayan.rankly.dto.auth.AuthUserResponse;
import com.sayan.rankly.dto.user.UpdatePreferencesRequest;
import com.sayan.rankly.dto.user.UserQuotaResponse;
import com.sayan.rankly.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/preferences")
    public ResponseEntity<AuthUserResponse> updatePreferences(
            Authentication authentication,
            @Valid @RequestBody UpdatePreferencesRequest request
    ) {
        AuthUserResponse updatedUser = userService.updatePreferences(authentication, request);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/quota")
    public ResponseEntity<UserQuotaResponse> getQuota(Authentication authentication) {
        UserQuotaResponse quota = userService.getDailyQuota(authentication);
        return ResponseEntity.ok(quota);
    }
}
