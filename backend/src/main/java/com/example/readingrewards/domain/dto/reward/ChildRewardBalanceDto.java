package com.example.readingrewards.domain.dto.reward;

import com.example.readingrewards.domain.model.reward.RewardTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ChildRewardBalanceDto(
    UUID childId,
    BalanceDto balance,
    List<RewardTypeSummaryDto> byRewardType
) {
    public record BalanceDto(
        BigDecimal totalEarned,
        BigDecimal totalPaid,
        BigDecimal availableBalance
    ) {}

    public record RewardTypeSummaryDto(
        RewardTemplate.RewardType rewardType,
        String description,
        BigDecimal totalEarned,
        BigDecimal totalPaid,
        BigDecimal availableBalance,
        String unitLabel,
        String accent
    ) {}
}
