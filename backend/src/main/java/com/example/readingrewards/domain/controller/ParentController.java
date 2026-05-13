package com.example.readingrewards.domain.controller;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.domain.dto.ResetChildPasswordRequest;
import com.example.readingrewards.domain.model.*;
import com.example.readingrewards.domain.repo.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/parent")
public class ParentController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BookReadRepository bookReadRepository;
    private final ChapterReadRepository chapterReadRepository;
    private final RewardRepository rewardRepository;
    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;

    public ParentController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            BookReadRepository bookReadRepository,
                            ChapterReadRepository chapterReadRepository,
                            RewardRepository rewardRepository,
                            BookRepository bookRepository,
                            ChapterRepository chapterRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bookReadRepository = bookReadRepository;
        this.chapterReadRepository = chapterReadRepository;
        this.rewardRepository = rewardRepository;
        this.bookRepository = bookRepository;
        this.chapterRepository = chapterRepository;
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

    @GetMapping("/{childId}/child-detail")
    public ResponseEntity<?> getChildDetail(@PathVariable UUID childId,
                                            @AuthenticationPrincipal UserDetails principal) {
        User parent = resolveParent(principal);
        if (parent == null) return ResponseEntity.status(403).body("Not authorized");

        User child = userRepository.findById(Objects.requireNonNull(childId)).orElse(null);
        if (child == null || !parent.getId().equals(child.getParentId())) {
            return ResponseEntity.status(404).body("Child not found or not your child");
        }

        List<BookRead> bookReads = bookReadRepository.findByUserId(child.getId());
        List<Reward> rewards = rewardRepository.findByUserId(child.getId());

        List<Map<String, Object>> books = bookReads.stream().map(br -> {
            String googleBookId = br.getGoogleBookId();
            if (googleBookId == null) return null;
            Book book = bookRepository.findById(googleBookId).orElse(null);
            if (book == null) return null;

            List<ChapterRead> chapterReads = chapterReadRepository.findByBookReadId(br.getId());
            List<Chapter> chapters = chapterRepository.findByGoogleBookIdOrderByChapterIndex(br.getGoogleBookId());

            List<Map<String, Object>> chaptersList = chapters.stream().map(ch -> {
                ChapterRead cr = chapterReads.stream()
                        .filter(r -> r.getChapterId().equals(ch.getId()))
                        .findFirst()
                        .orElse(null);
                Map<String, Object> chapterMap = new LinkedHashMap<>();
                chapterMap.put("id", ch.getId());
                chapterMap.put("index", ch.getChapterIndex());
                chapterMap.put("name", ch.getName());
                chapterMap.put("isRead", cr != null);
                if (cr != null) {
                    chapterMap.put("chapterReadId", cr.getId());
                    Reward reward = rewards.stream()
                            .filter(r -> cr.getId().equals(r.getChapterReadId()))
                            .findFirst()
                            .orElse(null);
                    if (reward != null) {
                        chapterMap.put("earnedReward", reward.getAmount());
                    }
                }
                return chapterMap;
            }).toList();

            Map<String, Object> bookMap = new LinkedHashMap<>();
            bookMap.put("bookReadId", br.getId());
            bookMap.put("googleBookId", book.getGoogleBookId());
            bookMap.put("title", book.getTitle());
            bookMap.put("authors", book.getAuthors());
            bookMap.put("thumbnailUrl", book.getThumbnailUrl());
            bookMap.put("startDate", br.getStartDate());
            bookMap.put("endDate", br.getEndDate());
            bookMap.put("inProgress", br.isInProgress());
            bookMap.put("bookEarningBasis", br.getBookEarningBasis());
            bookMap.put("pageCount", br.getPageCount());
            bookMap.put("pageCountConfirmed", Boolean.TRUE.equals(br.getPageCountConfirmed()));
            bookMap.put("chapters", chaptersList);
            return bookMap;
        }).filter(b -> b != null).toList();

        List<Map<String, Object>> rewardHistory = rewards.stream()
                .sorted(Comparator.comparing(Reward::getCreatedAt).reversed())
                .map(reward -> {
                    Map<String, Object> rewardMap = new LinkedHashMap<>();
                    rewardMap.put("id", reward.getId());
                    rewardMap.put("type", reward.getType());
                    rewardMap.put("amount", reward.getAmount());
                    rewardMap.put("chapterReadId", reward.getChapterReadId());
                    rewardMap.put("note", reward.getNote());
                    rewardMap.put("createdAt", reward.getCreatedAt());
                    return rewardMap;
                }).toList();

        double totalEarned = rewardRepository.getTotalEarnedByUserId(child.getId());
        double totalPaidOut = rewardRepository.getTotalPaidOutByUserId(child.getId());
        double totalSpent = rewardRepository.getTotalSpentByUserId(child.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("child", Map.of(
                "id", child.getId(),
                "firstName", child.getFirstName(),
                "username", child.getUsername()
        ));
        result.put("books", books);
        result.put("rewards", rewardHistory);
        result.put("totalEarned", totalEarned);
        result.put("currentBalance", totalEarned - totalPaidOut - totalSpent);

        return ResponseEntity.ok(result);
    }

    @Transactional
    @PostMapping("/{childId}/chapter-reads/{chapterReadId}/reverse")
    public ResponseEntity<?> reverseChapterRead(@PathVariable UUID childId,
                                               @PathVariable UUID chapterReadId,
                                               @AuthenticationPrincipal UserDetails principal) {
        User parent = resolveParent(principal);
        if (parent == null) return ResponseEntity.status(403).body("Not authorized");

        User child = userRepository.findById(Objects.requireNonNull(childId)).orElse(null);
        if (child == null || !parent.getId().equals(child.getParentId())) {
            return ResponseEntity.status(404).body("Child not found or not your child");
        }

        ChapterRead chapterRead = chapterReadRepository.findById(Objects.requireNonNull(chapterReadId)).orElse(null);
        if (chapterRead == null || !child.getId().equals(chapterRead.getUserId())) {
            return ResponseEntity.status(404).body("Chapter read not found or not child's read");
        }

        List<Reward> rewards = rewardRepository.findByUserId(child.getId()).stream()
                .filter(r -> chapterReadId.equals(r.getChapterReadId()))
                .collect(Collectors.toList());

        for (Reward reward : rewards) {
            rewardRepository.delete(Objects.requireNonNull(reward));
        }
        chapterReadRepository.delete(chapterRead);

        return ResponseEntity.ok().build();
    }
}
