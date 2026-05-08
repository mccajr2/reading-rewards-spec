package com.example.readingrewards.auth.controller;

import com.example.readingrewards.auth.dto.AuthUserResponse;
import com.example.readingrewards.auth.dto.LoginRequest;
import com.example.readingrewards.auth.dto.LoginResponse;
import com.example.readingrewards.auth.dto.SignupRequest;
import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.auth.service.VerificationEmailService;
import com.example.readingrewards.auth.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final VerificationEmailService verificationEmailService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${frontend.url}")
    private String frontendUrl;

    public AuthController(
        VerificationEmailService verificationEmailService,
        AuthenticationManager authenticationManager,
        UserRepository userRepository,
        JwtUtil jwtUtil,
        PasswordEncoder passwordEncoder
    ) {
        this.verificationEmailService = verificationEmailService;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest body) {
        if (body.email() == null || body.password() == null || body.firstName() == null || body.lastName() == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }
        if (userRepository.findByEmail(body.email()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already registered");
        }

        String verificationToken = UUID.randomUUID().toString();
        User user = new User();
        user.setEmail(body.email());
        user.setUsername(body.email());
        user.setPassword(passwordEncoder.encode(body.password()));
        user.setFirstName(body.firstName());
        user.setRole(User.UserRole.PARENT);
        user.setStatus("UNVERIFIED");
        user.setVerificationToken(verificationToken);
        userRepository.save(user);

        String subject = "Verify your Reading Rewards account";
        String htmlContent = "Welcome! Please verify your account by clicking: "
            + "<a href='" + frontendUrl + "/verify-email?token=" + verificationToken + "'>Verify Account</a>";
        boolean sent = verificationEmailService.sendEmail(user.getEmail(), subject, htmlContent);
        if (!sent) {
            return ResponseEntity.accepted().body("Signup successful, but verification email could not be sent right now.");
        }

        return ResponseEntity.ok("Signup successful. Please check your email to verify your account.");
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        User user = userRepository.findByVerificationToken(token).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("Invalid or expired token");
        }

        user.setStatus("VERIFIED");
        user.setVerificationToken(null);
        userRepository.save(user);
        return ResponseEntity.ok("Email verified. You can now log in.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {
        if (body.username() == null || body.password() == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(body.username(), body.password())
            );

            User user;
            if (body.username().contains("@")) {
                user = userRepository.findByEmail(body.username()).orElseThrow();
            } else {
                user = userRepository.findByUsername(body.username()).orElseThrow();
            }

            if (user.getRole() == User.UserRole.PARENT
                && (user.getStatus() == null || !"VERIFIED".equals(user.getStatus()))) {
                return ResponseEntity.status(403).body("Parent account not verified. Please check your email.");
            }

            String token = jwtUtil.generateToken(body.username());
            LoginResponse response = new LoginResponse(token, toAuthUserResponse(user));
            return ResponseEntity.ok(response);
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok().build();
    }

    private AuthUserResponse toAuthUserResponse(User user) {
        return new AuthUserResponse(
            user.getId(),
            user.getRole(),
            user.getParentId(),
            user.getEmail(),
            user.getUsername(),
            user.getFirstName(),
            user.getStatus()
        );
    }
}