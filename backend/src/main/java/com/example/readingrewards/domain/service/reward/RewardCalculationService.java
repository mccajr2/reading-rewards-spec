package com.example.readingrewards.domain.service.reward;

import com.example.readingrewards.domain.model.reward.RewardTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RewardCalculationService {

    public BigDecimal calculateAmount(BigDecimal rate, int units) {
        if (rate == null || units < 0) {
            throw new IllegalArgumentException("Rate must be non-null and units must be >= 0");
        }
        return rate.multiply(BigDecimal.valueOf(units)).setScale(2, RoundingMode.HALF_UP);
    }

    public String formatCalculationNote(RewardTemplate.RewardType type, BigDecimal rate, int units) {
        BigDecimal total = calculateAmount(rate, units);
        return switch (type) {
            case MONEY -> String.format("%d units @ $%s = $%s", units, rate, total);
            case TIME -> String.format("%d units @ %s min = %s min", units, rate, total);
            case CUSTOM_TEXT -> String.format("%d units earned", units);
        };
    }
}
