package com.example.readingrewards.unit.reward;

import com.example.readingrewards.domain.service.reward.CannedRewardTemplateProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CannedRewardTemplateTest {

    private final CannedRewardTemplateProvider provider = new CannedRewardTemplateProvider();

    @Test
    void requiredTemplatesArePresent() {
        var ids = provider.defaults().stream().map(CannedRewardTemplateProvider.CannedRewardTemplate::cannedTemplateId).toList();
        assertTrue(ids.contains("MONEY_050_PER_CHAPTER"));
        assertTrue(ids.contains("MONEY_500_PER_BOOK"));
        assertTrue(ids.contains("TIME_5MIN_PER_CHAPTER"));
        assertTrue(ids.contains("TIME_120MIN_PER_BOOK"));
    }
}
