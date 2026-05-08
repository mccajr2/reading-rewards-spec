package com.example.readingrewards.domain.controller;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.domain.dto.ResetChildPasswordRequest;
import com.example.readingrewards.domain.model.RewardType;
import com.example.readingrewards.domain.repo.BookReadRepository;
import com.example.readingrewards.domain.repo.ChapterReadRepository;
import com.example.readingrewards.domain.repo.RewardRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/parent")
public class ParentController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BookReadRepository bookReadRepository;
    private final ChapterReadRepository chapterReadRepository;
    private final RewardRepository rewardRepository;

    public ParentController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            BookReadRepository bookReadRepository,
                            ChapterReadRepository chapterReadRepository,
                            RewardRepository rewardRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bookReadRepository = bookReadRepository;
        this.chapterReadRepository = chapterReadRepository;
        this.rewardRepository = rewardRepository;
    }

    private User resolveParent(UserDetails principal) {
        String identifier = principal.getUsername();
        User parent = identifier.contains("@")
                ? userRepository.findByEmail(identifier).orElse(null)
                : userRepository.findByUsername(identifier).orElse(null);
        if (parent == null || parent.getRole() != User.UserRole.PARENT) return null;
        return parent;
    }

    @GetMapping("/kids")
    public ResponseEntity<?> getKids(@AuthenticationPrincipal UserDetails principal) {
        User parent = resolveParent(principal);
        if (parent == null) return ResponseEntity.status(403).body("Not authorized");
        List<User> kids = userRepository.findByParentId(parent.getId());
        List<Map<String, Object>> result = kids.stream().map(kid -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", kid.getId());
            m.put("firstName", kid.getFirstName());
            m.put("username", kid.getUsername());
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/kids")
    public ResponseEntity<?> addKid(@AuthenticationPrincipal UserDetails principal,
                                    @RequestBody Map<String, String> body) {
        User parent = resolveParent(principal);
        if (parent == null) return ResponseEntity.status(403).body("Not authorized");

        String username = body.get("username");
        String firstName = body.get("firstName");
        String password = body.get("password");
        if (username == null || firstName == null || password == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Username already taken");
        }

        User kid = new User();
        kid.setUsername(username);
        kid.setFirstName(firstName);
        kid.setPassword(passwordEncoder.encode(password));
        kid.setRole(User.UserRole.CHILD);
        kid.setStatus("VERIFIED");
        kid.setParentId(parent.getId());
        userRepository.save(kid);
        return ResponseEntity.ok("Child account created");
    }

    @PostMapping("/reset-child-password")
    public ResponseEntity<?> resetChildPassword(@AuthenticationPrincipal UserDetails principal,
                                                @RequestBody ResetChildPasswordRequest req) {
        User parent = resolveParent(principal);
        if (parent == null) return ResponseEntity.status(403).body("Not authorized");

        User child = userRepository.findByUsername(req.getChildUsername()).orElse(null);
        if (child == null || !parent.getId().equals(child.getParentId())) {
            return ResponseEntity.status(404).body("Child not found or not your child");
        }
        child.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(child);
        return ResponseEntity.ok("Child password reset successfully");
    }

    @GetMapping("/kids/summary")
    public ResponseEntity<?> kidsSummary(@AuthenticationPrincipal UserDetails principal) {
        User parent = resolveParent(principal);
        if (parent == null) return ResponseEntity.status(403).body("Not authorized");
        List<User> kids = userRepository.findByParentId(parent.getId());
        List<Map<String, Object>> result = kids.stream().map(kid -> {
            long booksRead = bookReadRepository.findByUserId(kid.getId()).size();
            long chaptersRead = chapterReadRepository.findByUserId(kid.getId()).size();
            double totalEarned = rewardRepository.getTotalEarnedByUserId(kid.getId());
            double totalPaidOut = rewardRepository.getTotalPaidOutByUserId(kid.getId());
            double totalSpent = rewardRepository.getTotalSpentByUserId(kid.getId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", kid.getId());
            m.put("firstName", kid.getFirstName());
            m.put("username", kid.getUsername());
            m.put("booksRead", booksRead);
            m.put("chaptersRead", chaptersRead);
            m.put("totalEarned", totalEarned);
            m.put("currentBalance", totalEarned - totalPaidOut - totalSpent);
            return m;
        }).toList();
        return ResponseEntity.ok(Map.of("kids", result));
    }
}
