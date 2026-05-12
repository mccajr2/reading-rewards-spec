package com.example.readingrewards.domain.controller;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.domain.dto.reward.RewardDtos;
import com.example.readingrewards.domain.model.reward.RewardTemplate;
import com.example.readingrewards.domain.service.reward.CannedRewardTemplateProvider;
import com.example.readingrewards.domain.service.reward.PerChildRewardService;
import com.example.readingrewards.domain.service.reward.ParentRewardConfigService;
import com.example.readingrewards.domain.service.reward.RewardTemplateArchiveService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/parent/rewards")
public class ParentRewardController {

    private final UserRepository userRepository;
    private final ParentRewardConfigService parentRewardConfigService;
    private final PerChildRewardService perChildRewardService;
    private final RewardTemplateArchiveService rewardTemplateArchiveService;
    private final CannedRewardTemplateProvider cannedRewardTemplateProvider;

    public ParentRewardController(
        UserRepository userRepository,
        ParentRewardConfigService parentRewardConfigService,
        PerChildRewardService perChildRewardService,
        RewardTemplateArchiveService rewardTemplateArchiveService,
        CannedRewardTemplateProvider cannedRewardTemplateProvider
    ) {
        this.userRepository = userRepository;
        this.parentRewardConfigService = parentRewardConfigService;
        this.perChildRewardService = perChildRewardService;
        this.rewardTemplateArchiveService = rewardTemplateArchiveService;
        this.cannedRewardTemplateProvider = cannedRewardTemplateProvider;
    }

    @GetMapping("/canned")
    public ResponseEntity<?> listCannedRewards(@AuthenticationPrincipal UserDetails principal) {
        User parent = resolveParent(principal);
        if (parent == null) {
            return ResponseEntity.status(403).body("Not authorized");
        }
        return ResponseEntity.ok(cannedRewardTemplateProvider.defaults());
    }

    @GetMapping
    public ResponseEntity<?> listRewards(@AuthenticationPrincipal UserDetails principal) {
        User parent = resolveParent(principal);
        if (parent == null) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        List<RewardDtos.RewardTemplateDto> familyRewards = parentRewardConfigService.listFamilyRewards(parent.getId())
            .stream()
            .map(this::toDto)
            .toList();

        List<RewardDtos.PerChildRewardsDto> perChildRewards = userRepository.findByParentId(parent.getId())
            .stream()
            .map(child -> new RewardDtos.PerChildRewardsDto(
                child.getId(),
                child.getFirstName() != null ? child.getFirstName() : child.getUsername(),
                perChildRewardService.listPerChildRewards(parent.getId(), child.getId()).stream().map(this::toDto).toList()
            ))
            .toList();

        return ResponseEntity.ok(new RewardDtos.ParentRewardsResponseDto(familyRewards, perChildRewards));
    }

    @PostMapping
    public ResponseEntity<?> createFamilyReward(
        @AuthenticationPrincipal UserDetails principal,
        @RequestBody RewardTemplateRequest request
    ) {
        User parent = resolveParent(principal);
        if (parent == null) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        RewardTemplate created = parentRewardConfigService.createFamilyReward(parent.getId(), toEntity(request));
        return ResponseEntity.status(201).body(toDto(created));
    }

    @PutMapping("/{rewardTemplateId}")
    public ResponseEntity<?> updateFamilyReward(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID rewardTemplateId,
        @RequestBody RewardTemplateRequest request
    ) {
        User parent = resolveParent(principal);
        if (parent == null) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        RewardTemplate updated = parentRewardConfigService.updateFamilyReward(parent.getId(), rewardTemplateId, toEntity(request));
        return ResponseEntity.ok(toDto(updated));
    }

    @PostMapping("/child/{childId}")
    public ResponseEntity<?> createPerChildReward(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID childId,
        @RequestBody RewardTemplateRequest request
    ) {
        User parent = resolveParent(principal);
        if (parent == null) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        RewardTemplate created = perChildRewardService.createPerChildReward(parent.getId(), childId, toEntity(request));
        return ResponseEntity.status(201).body(toDto(created));
    }

    @DeleteMapping("/{rewardTemplateId}")
    public ResponseEntity<?> archiveFamilyReward(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID rewardTemplateId
    ) {
        User parent = resolveParent(principal);
        if (parent == null) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        RewardTemplate archived = rewardTemplateArchiveService.archiveFamilyReward(parent.getId(), rewardTemplateId);
        return ResponseEntity.ok(toDto(archived));
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

    private RewardTemplate toEntity(RewardTemplateRequest request) {
        RewardTemplate template = new RewardTemplate();
        template.setChildId(request.childId());
        template.setRewardType(request.rewardType());
        template.setAmount(request.amount());
        template.setUnit(request.unit());
        template.setFrequency(request.frequency());
        template.setCannedTemplateId(request.cannedTemplateId());
        template.setDescription(request.description());
        return template;
    }

    private RewardDtos.RewardTemplateDto toDto(RewardTemplate template) {
        return new RewardDtos.RewardTemplateDto(
            template.getId(),
            template.getChildId(),
            template.getRewardType(),
            template.getAmount(),
            template.getUnit(),
            template.getDescription(),
            template.isDeleted(),
            template.getCreatedAt()
        );
    }

    public record RewardTemplateRequest(
        UUID childId,
        RewardTemplate.RewardType rewardType,
        java.math.BigDecimal amount,
        RewardTemplate.RewardUnit unit,
        RewardTemplate.RewardFrequency frequency,
        String cannedTemplateId,
        String description
    ) {}
}
