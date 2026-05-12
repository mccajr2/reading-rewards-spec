package com.example.readingrewards.domain.service.reward;

import com.example.readingrewards.domain.dto.reward.ChildRewardBalanceDto;
import com.example.readingrewards.domain.model.reward.RewardAccumulation;
import com.example.readingrewards.domain.model.reward.RewardTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class RewardTypeFormattingService {

    public List<ChildRewardBalanceDto.RewardTypeSummaryDto> summarizeByType(List<RewardAccumulation> accumulations) {
        Map<RewardTemplate.RewardType, Totals> totalsByType = new EnumMap<>(RewardTemplate.RewardType.class);

        for (RewardAccumulation accumulation : accumulations) {
            RewardTemplate.RewardType type = accumulation.getRewardType() == null
                ? RewardTemplate.RewardType.MONEY
                : accumulation.getRewardType();
            Totals totals = totalsByType.computeIfAbsent(type, ignored -> new Totals());

            BigDecimal amount = accumulation.getAmountEarned() == null ? BigDecimal.ZERO : accumulation.getAmountEarned();
            if (accumulation.getStatus() == RewardAccumulation.AccumulationStatus.PAID) {
                totals.totalPaid = totals.totalPaid.add(amount);
                totals.totalEarned = totals.totalEarned.add(amount);
            } else {
                totals.totalEarned = totals.totalEarned.add(amount);
            }
        }

        List<ChildRewardBalanceDto.RewardTypeSummaryDto> summaries = new ArrayList<>();
        for (RewardTemplate.RewardType type : RewardTemplate.RewardType.values()) {
            Totals totals = totalsByType.get(type);
            if (totals == null) {
                continue;
            }
            summaries.add(new ChildRewardBalanceDto.RewardTypeSummaryDto(
                type,
                description(type),
                totals.totalEarned,
                totals.totalPaid,
                totals.totalEarned.subtract(totals.totalPaid),
                unitLabel(type),
                accent(type)
            ));
        }
        return summaries;
    }

    public String description(RewardTemplate.RewardType type) {
        return switch (type) {
            case MONEY -> "Cash Rewards";
            case TIME -> "Time Rewards";
            case CUSTOM_TEXT -> "Custom Rewards";
        };
    }

    public String unitLabel(RewardTemplate.RewardType type) {
        return switch (type) {
            case MONEY -> "USD";
            case TIME -> "minutes";
            case CUSTOM_TEXT -> "points";
        };
    }

    public String accent(RewardTemplate.RewardType type) {
        return switch (type) {
            case MONEY -> "money";
            case TIME -> "time";
            case CUSTOM_TEXT -> "custom";
        };
    }

    private static final class Totals {
        private BigDecimal totalEarned = BigDecimal.ZERO;
        private BigDecimal totalPaid = BigDecimal.ZERO;
    }
}
