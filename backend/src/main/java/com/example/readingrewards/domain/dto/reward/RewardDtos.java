package com.example.readingrewards.domain.dto.reward;

import com.example.readingrewards.domain.model.reward.RewardTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class RewardDtos {

    private RewardDtos() {
    }

    public record RewardTemplateDto(
        UUID rewardTemplateId,
        UUID childId,
        RewardTemplate.RewardType rewardType,
        BigDecimal amount,
        RewardTemplate.RewardUnit unit,
        String description,
        boolean isDeleted,
        LocalDateTime createdAt
    ) {}

    public record RewardSelectionDto(
        UUID selectionId,
        UUID bookReadId,
        UUID rewardTemplateId,
        BigDecimal lockedAmount,
        RewardTemplate.RewardUnit lockedUnit,
        LocalDateTime selectedAt,
        boolean isActive
    ) {}

    public record RewardBalanceDto(
        BigDecimal totalEarned,
        BigDecimal totalPaid,
        BigDecimal availableBalance
    ) {}

    public record ParentRewardsResponseDto(
        List<RewardTemplateDto> familyRewards,
        List<PerChildRewardsDto> perChildRewards
    ) {}

    public record PerChildRewardsDto(
        UUID childId,
        String childName,
        List<RewardTemplateDto> rewards
    ) {}
}
