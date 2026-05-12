package com.example.readingrewards.unit.reward;

import com.example.readingrewards.domain.model.reward.ProgressTracking;
import com.example.readingrewards.domain.model.reward.RewardTemplate;
import com.example.readingrewards.domain.service.reward.ProgressTrackingPolicyService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProgressTrackingValidationTest {

    private final ProgressTrackingPolicyService service = new ProgressTrackingPolicyService();

    @Test
    void perChapterRequiresChapterTracking() {
        ProgressTracking tracking = new ProgressTracking();
        tracking.setTrackingType(ProgressTracking.TrackingType.NONE);

        assertThrows(IllegalArgumentException.class, () ->
            service.validateCompletionAllowed(RewardTemplate.RewardUnit.PER_CHAPTER, tracking)
        );
    }

    @Test
    void perBookAllowsNoTracking() {
        ProgressTracking tracking = new ProgressTracking();
        tracking.setTrackingType(ProgressTracking.TrackingType.NONE);

        assertDoesNotThrow(() ->
            service.validateCompletionAllowed(RewardTemplate.RewardUnit.PER_BOOK, tracking)
        );
    }

    @Test
    void chapterTrackingRequiresPositiveTotal() {
        ProgressTracking tracking = new ProgressTracking();
        tracking.setTrackingType(ProgressTracking.TrackingType.CHAPTERS);
        tracking.setTotalChapters(0);

        assertThrows(IllegalArgumentException.class, () -> service.validateTrackingConsistency(tracking));
    }
}
