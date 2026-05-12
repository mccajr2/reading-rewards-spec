package com.example.readingrewards.domain.controller;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.domain.model.BookRead;
import com.example.readingrewards.domain.model.reward.RewardSelection;
import com.example.readingrewards.domain.model.reward.RewardTemplate;
import com.example.readingrewards.domain.repo.BookReadRepository;
import com.example.readingrewards.domain.repo.reward.RewardSelectionRepository;
import com.example.readingrewards.domain.repo.reward.RewardTemplateRepository;
import com.example.readingrewards.domain.service.reward.ChildRewardAvailabilityService;
import com.example.readingrewards.domain.service.reward.RewardSelectionPolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/child/rewards")
public class ChildRewardSelectionController {

    private final UserRepository userRepository;
    private final BookReadRepository bookReadRepository;
    private final RewardTemplateRepository rewardTemplateRepository;
    private final RewardSelectionRepository rewardSelectionRepository;
    private final ChildRewardAvailabilityService childRewardAvailabilityService;
    private final RewardSelectionPolicyService rewardSelectionPolicyService;

    public ChildRewardSelectionController(
        UserRepository userRepository,
        BookReadRepository bookReadRepository,
        RewardTemplateRepository rewardTemplateRepository,
        RewardSelectionRepository rewardSelectionRepository,
        ChildRewardAvailabilityService childRewardAvailabilityService,
        RewardSelectionPolicyService rewardSelectionPolicyService
    ) {
        this.userRepository = userRepository;
        this.bookReadRepository = bookReadRepository;
        this.rewardTemplateRepository = rewardTemplateRepository;
        this.rewardSelectionRepository = rewardSelectionRepository;
        this.childRewardAvailabilityService = childRewardAvailabilityService;
        this.rewardSelectionPolicyService = rewardSelectionPolicyService;
    }

    @GetMapping("/available")
    public ResponseEntity<?> available(@AuthenticationPrincipal UserDetails principal) {
        User child = resolveChild(principal);
        if (child == null) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        List<Map<String, Object>> rewards = childRewardAvailabilityService.listAvailableRewards(child)
            .stream()
            .map(template -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("rewardTemplateId", template.getId());
                row.put("rewardType", template.getRewardType());
                row.put("amount", template.getAmount());
                row.put("unit", template.getUnit());
                row.put("frequency", template.getFrequency());
                row.put("description", template.getDescription());
                row.put("cannedTemplateId", template.getCannedTemplateId());
                row.put("scope", template.getChildId() == null ? "FAMILY" : "PER_CHILD");
                return row;
            })
            .toList();

        Object defaultRewardId = rewards.isEmpty() ? null : rewards.get(0).get("rewardTemplateId");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("childId", child.getId());
        payload.put("availableRewards", rewards);
        payload.put("defaultRewardId", defaultRewardId);
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/select/{bookReadId}")
    public ResponseEntity<?> select(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID bookReadId,
        @RequestBody RewardSelectionRequest request
    ) {
        User child = resolveChild(principal);
        if (child == null) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        Optional<RewardSelection> existing = rewardSelectionRepository.findByBookReadIdAndActiveTrue(bookReadId);
        if (existing.isPresent()) {
            return ResponseEntity.status(409).body("Reward already selected for this book");
        }

        RewardSelection created = createSelection(child, bookReadId, request.rewardTemplateId());
        return ResponseEntity.status(201).body(toResponse(created));
    }

    @PutMapping("/select/{bookReadId}")
    public ResponseEntity<?> changeSelection(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID bookReadId,
        @RequestBody RewardSelectionRequest request
    ) {
        User child = resolveChild(principal);
        if (child == null) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        rewardSelectionRepository.findByBookReadIdAndActiveTrue(bookReadId).ifPresent(active -> {
            active.setActive(false);
            rewardSelectionRepository.save(active);
        });

        RewardSelection created = createSelection(child, bookReadId, request.rewardTemplateId());
        return ResponseEntity.ok(toResponse(created));
    }

    private RewardSelection createSelection(User child, UUID bookReadId, UUID rewardTemplateId) {
        Objects.requireNonNull(bookReadId, "bookReadId is required");
        Objects.requireNonNull(rewardTemplateId, "rewardTemplateId is required");

        BookRead bookRead = bookReadRepository.findById(bookReadId)
            .orElseThrow(() -> new IllegalArgumentException("Book read not found"));
        if (!child.getId().equals(bookRead.getUserId())) {
            throw new IllegalArgumentException("Book read not found for this child");
        }

        RewardTemplate template = rewardTemplateRepository.findById(rewardTemplateId)
            .orElseThrow(() -> new IllegalArgumentException("Reward template not found"));

        if (!rewardSelectionPolicyService.canSelectTemplate(template)) {
            throw new IllegalArgumentException("Reward template not available for selection");
        }

        RewardSelection selection = new RewardSelection();
        selection.setChildId(child.getId());
        selection.setBookReadId(bookReadId);
        selection.setRewardTemplateId(template.getId());
        selection.setLockedAmount(template.getAmount());
        selection.setLockedUnit(template.getUnit());
        selection.setActive(true);
        return rewardSelectionRepository.save(selection);
    }

    private Map<String, Object> toResponse(RewardSelection selection) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("selectionId", selection.getId());
        payload.put("bookReadId", selection.getBookReadId());
        payload.put("rewardTemplateId", selection.getRewardTemplateId());
        payload.put("lockedAmount", selection.getLockedAmount());
        payload.put("lockedUnit", selection.getLockedUnit());
        payload.put("selectedAt", selection.getSelectedAt());
        payload.put("isActive", selection.isActive());
        return payload;
    }

    private User resolveChild(UserDetails principal) {
        if (principal == null) {
            return null;
        }
        String identifier = principal.getUsername();
        User child = identifier.contains("@")
            ? userRepository.findByEmail(identifier).orElse(null)
            : userRepository.findByUsername(identifier).orElse(null);
        if (child == null || child.getRole() != User.UserRole.CHILD) {
            return null;
        }
        return child;
    }

    public record RewardSelectionRequest(UUID rewardTemplateId) {}
}
