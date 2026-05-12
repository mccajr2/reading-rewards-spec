package com.example.readingrewards.domain.controller;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.domain.model.reward.RewardAccumulation;
import com.example.readingrewards.domain.repo.reward.RewardAccumulationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Objects;

@RestController
@RequestMapping("/api/parent/rewards")
public class ParentPayoutController {

    private final UserRepository userRepository;
    private final RewardAccumulationRepository rewardAccumulationRepository;

    public ParentPayoutController(UserRepository userRepository, RewardAccumulationRepository rewardAccumulationRepository) {
        this.userRepository = userRepository;
        this.rewardAccumulationRepository = rewardAccumulationRepository;
    }

    @GetMapping("/child/{childId}/accumulation")
    public ResponseEntity<?> childAccumulation(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID childId,
        @RequestParam(required = false) RewardAccumulation.AccumulationStatus status
    ) {
        Objects.requireNonNull(childId, "childId is required");
        User parent = resolveParent(principal);
        if (parent == null) return ResponseEntity.status(403).body("Not authorized");

        User child = userRepository.findById(childId).orElse(null);
        if (child == null || !parent.getId().equals(child.getParentId())) {
            return ResponseEntity.status(404).body("Child not found");
        }

        List<RewardAccumulation> rows = status == null
            ? rewardAccumulationRepository.findByChildIdOrderByCreatedAtDesc(childId)
            : rewardAccumulationRepository.findByChildIdAndStatusOrderByCreatedAtDesc(childId, status);

        BigDecimal totalEarned = rewardAccumulationRepository.sumAmountByChildId(childId, RewardAccumulation.AccumulationStatus.EARNED)
            .add(rewardAccumulationRepository.sumAmountByChildId(childId, RewardAccumulation.AccumulationStatus.PAID));
        BigDecimal totalPaid = rewardAccumulationRepository.sumAmountByChildId(childId, RewardAccumulation.AccumulationStatus.PAID);
        BigDecimal available = totalEarned.subtract(totalPaid);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalEarned", totalEarned);
        summary.put("totalPaid", totalPaid);
        summary.put("availableBalance", available);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("childId", child.getId());
        response.put("childName", child.getFirstName() != null ? child.getFirstName() : child.getUsername());
        response.put("summary", summary);
        response.put("accumulations", rows);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/child/{childId}/payout-confirm")
    public ResponseEntity<?> confirmPayout(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID childId,
        @RequestBody PayoutConfirmRequest request
    ) {
        Objects.requireNonNull(childId, "childId is required");
        User parent = resolveParent(principal);
        if (parent == null) return ResponseEntity.status(403).body("Not authorized");

        User child = userRepository.findById(childId).orElse(null);
        if (child == null || !parent.getId().equals(child.getParentId())) {
            return ResponseEntity.status(404).body("Child not found");
        }

        BigDecimal paidAmount = BigDecimal.ZERO;
        List<UUID> applied = new ArrayList<>();
        for (UUID id : request.accumulationIds()) {
            if (id == null) {
                continue;
            }
            RewardAccumulation row = rewardAccumulationRepository.findById(id).orElse(null);
            if (row == null || !childId.equals(row.getChildId())) {
                continue;
            }
            row.setStatus(RewardAccumulation.AccumulationStatus.PAID);
            row.setPayoutDate(LocalDateTime.now());
            rewardAccumulationRepository.save(row);
            paidAmount = paidAmount.add(row.getAmountEarned());
            applied.add(row.getId());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("childId", childId);
        response.put("amount", paidAmount);
        response.put("accumulationIds", applied);
        response.put("status", "PAID");
        response.put("confirmedAt", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    private User resolveParent(UserDetails principal) {
        if (principal == null) {
            return null;
        }

        String identifier = principal.getUsername();
        User parent = identifier.contains("@")
            ? userRepository.findByEmail(identifier).orElse(null)
            : userRepository.findByUsername(identifier).orElse(null);

        if (parent == null || parent.getRole() != User.UserRole.PARENT) {
            return null;
        }
        return parent;
    }

    public record PayoutConfirmRequest(List<UUID> accumulationIds, String payoutMethod) {}
}
