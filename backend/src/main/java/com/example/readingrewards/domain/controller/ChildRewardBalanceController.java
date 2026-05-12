package com.example.readingrewards.domain.controller;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.domain.dto.reward.ChildRewardBalanceDto;
import com.example.readingrewards.domain.model.reward.RewardAccumulation;
import com.example.readingrewards.domain.repo.reward.RewardAccumulationRepository;
import com.example.readingrewards.domain.service.reward.RewardTypeFormattingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/child/rewards")
public class ChildRewardBalanceController {

    private final UserRepository userRepository;
    private final RewardAccumulationRepository rewardAccumulationRepository;
    private final RewardTypeFormattingService rewardTypeFormattingService;

    public ChildRewardBalanceController(
        UserRepository userRepository,
        RewardAccumulationRepository rewardAccumulationRepository,
        RewardTypeFormattingService rewardTypeFormattingService
    ) {
        this.userRepository = userRepository;
        this.rewardAccumulationRepository = rewardAccumulationRepository;
        this.rewardTypeFormattingService = rewardTypeFormattingService;
    }

    @GetMapping("/balance")
    public ResponseEntity<?> balance(@AuthenticationPrincipal UserDetails principal) {
        User child = resolveChild(principal);
        if (child == null) return ResponseEntity.status(403).body("Not authorized");

        BigDecimal totalEarned = rewardAccumulationRepository.sumAmountByChildId(child.getId(), RewardAccumulation.AccumulationStatus.EARNED)
            .add(rewardAccumulationRepository.sumAmountByChildId(child.getId(), RewardAccumulation.AccumulationStatus.PAID));
        BigDecimal totalPaid = rewardAccumulationRepository.sumAmountByChildId(child.getId(), RewardAccumulation.AccumulationStatus.PAID);
        BigDecimal available = totalEarned.subtract(totalPaid);

        List<RewardAccumulation> accumulations = rewardAccumulationRepository.findByChildIdOrderByCreatedAtDesc(child.getId());
        List<ChildRewardBalanceDto.RewardTypeSummaryDto> byType = rewardTypeFormattingService.summarizeByType(accumulations);

        return ResponseEntity.ok(new ChildRewardBalanceDto(
            child.getId(),
            new ChildRewardBalanceDto.BalanceDto(totalEarned, totalPaid, available),
            byType
        ));
    }

    @GetMapping("/history")
    public ResponseEntity<?> history(
        @AuthenticationPrincipal UserDetails principal,
        @RequestParam(required = false) RewardAccumulation.AccumulationStatus status
    ) {
        User child = resolveChild(principal);
        if (child == null) return ResponseEntity.status(403).body("Not authorized");

        List<RewardAccumulation> rows = status == null
            ? rewardAccumulationRepository.findByChildIdOrderByCreatedAtDesc(child.getId())
            : rewardAccumulationRepository.findByChildIdAndStatusOrderByCreatedAtDesc(child.getId(), status);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("childId", child.getId());
        response.put("accumulations", rows);
        return ResponseEntity.ok(response);
    }

    private User resolveChild(UserDetails principal) {
        if (principal == null) return null;
        String identifier = principal.getUsername();
        User child = identifier.contains("@")
            ? userRepository.findByEmail(identifier).orElse(null)
            : userRepository.findByUsername(identifier).orElse(null);
        if (child == null || child.getRole() != User.UserRole.CHILD) return null;
        return child;
    }
}
