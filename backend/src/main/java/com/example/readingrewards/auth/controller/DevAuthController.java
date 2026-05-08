package com.example.readingrewards.auth.controller;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev/test only endpoints — disabled in production via @Profile("!prod").
 * Allows E2E tests to force-verify a user without a real email flow.
 */
@Profile("!prod")
@RestController
@RequestMapping("/api/auth/dev")
public class DevAuthController {

    private final UserRepository userRepository;

    public DevAuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Force-verify a user account by email. Only available outside prod profile.
     * Used by Playwright E2E tests after signup to bypass the email verification step.
     */
    @PostMapping("/verify")
    public ResponseEntity<String> forceVerify(@RequestParam String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        user.setStatus("VERIFIED");
        user.setVerificationToken(null);
        userRepository.save(user);
        return ResponseEntity.ok("verified");
    }
}
