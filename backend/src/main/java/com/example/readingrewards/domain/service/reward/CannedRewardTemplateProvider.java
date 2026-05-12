package com.example.readingrewards.domain.service.reward;

import com.example.readingrewards.domain.model.reward.RewardTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CannedRewardTemplateProvider {

    public record CannedRewardTemplate(
        String cannedTemplateId,
        RewardTemplate.RewardType rewardType,
        BigDecimal amount,
        RewardTemplate.RewardUnit unit,
        RewardTemplate.RewardFrequency frequency,
        String description
    ) {}

    public List<CannedRewardTemplate> defaults() {
        return List.of(
            new CannedRewardTemplate(
                "MONEY_050_PER_CHAPTER",
                RewardTemplate.RewardType.MONEY,
                BigDecimal.valueOf(0.50),
                RewardTemplate.RewardUnit.PER_CHAPTER,
                RewardTemplate.RewardFrequency.IMMEDIATE,
                "$0.50 per chapter"
            ),
            new CannedRewardTemplate(
                "MONEY_500_PER_BOOK",
                RewardTemplate.RewardType.MONEY,
                BigDecimal.valueOf(5.00),
                RewardTemplate.RewardUnit.PER_BOOK,
                RewardTemplate.RewardFrequency.ON_COMPLETION,
                "$5 per book"
            ),
            new CannedRewardTemplate(
                "TIME_5MIN_PER_CHAPTER",
                RewardTemplate.RewardType.TIME,
                BigDecimal.valueOf(5),
                RewardTemplate.RewardUnit.PER_CHAPTER,
                RewardTemplate.RewardFrequency.IMMEDIATE,
                "5 minutes screentime per chapter"
            ),
            new CannedRewardTemplate(
                "TIME_120MIN_PER_BOOK",
                RewardTemplate.RewardType.TIME,
                BigDecimal.valueOf(120),
                RewardTemplate.RewardUnit.PER_BOOK,
                RewardTemplate.RewardFrequency.ON_COMPLETION,
                "2 hours screentime per book"
            )
        );
    }
}
