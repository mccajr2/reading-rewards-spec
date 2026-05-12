package com.example.readingrewards.unit.reward;

import com.example.readingrewards.domain.model.reward.RewardAccumulation;
import com.example.readingrewards.domain.model.reward.RewardTemplate;
import com.example.readingrewards.domain.service.reward.RewardTypeFormattingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RewardTypeFormattingServiceTest {

    private final RewardTypeFormattingService service = new RewardTypeFormattingService();

    @Test
    void aggregatesMoneyTimeAndCustomTypesWithStatusRules() {
        RewardAccumulation earnedMoney = accumulation(RewardTemplate.RewardType.MONEY, "3.50", RewardAccumulation.AccumulationStatus.EARNED);
        RewardAccumulation paidMoney = accumulation(RewardTemplate.RewardType.MONEY, "1.25", RewardAccumulation.AccumulationStatus.PAID);
        RewardAccumulation earnedTime = accumulation(RewardTemplate.RewardType.TIME, "15.00", RewardAccumulation.AccumulationStatus.EARNED);
        RewardAccumulation earnedCustom = accumulation(RewardTemplate.RewardType.CUSTOM_TEXT, "2.00", RewardAccumulation.AccumulationStatus.EARNED);

        var summaries = service.summarizeByType(List.of(earnedMoney, paidMoney, earnedTime, earnedCustom));

        assertThat(summaries).hasSize(3);
        var money = summaries.stream().filter(s -> s.rewardType() == RewardTemplate.RewardType.MONEY).findFirst().orElseThrow();
        assertThat(money.totalEarned()).isEqualByComparingTo("4.75");
        assertThat(money.totalPaid()).isEqualByComparingTo("1.25");
        assertThat(money.availableBalance()).isEqualByComparingTo("3.50");
        assertThat(money.description()).isEqualTo("Cash Rewards");

        var time = summaries.stream().filter(s -> s.rewardType() == RewardTemplate.RewardType.TIME).findFirst().orElseThrow();
        assertThat(time.totalEarned()).isEqualByComparingTo("15.00");
        assertThat(time.totalPaid()).isEqualByComparingTo("0.00");
        assertThat(time.unitLabel()).isEqualTo("minutes");

        var custom = summaries.stream().filter(s -> s.rewardType() == RewardTemplate.RewardType.CUSTOM_TEXT).findFirst().orElseThrow();
        assertThat(custom.totalEarned()).isEqualByComparingTo("2.00");
        assertThat(custom.unitLabel()).isEqualTo("points");
    }

    private static RewardAccumulation accumulation(
        RewardTemplate.RewardType type,
        String amount,
        RewardAccumulation.AccumulationStatus status
    ) {
        RewardAccumulation accumulation = new RewardAccumulation();
        accumulation.setRewardType(type);
        accumulation.setAmountEarned(new BigDecimal(amount));
        accumulation.setStatus(status);
        return accumulation;
    }
}
