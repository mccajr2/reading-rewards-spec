package com.example.readingrewards.domain.service.reward;

import com.example.readingrewards.domain.model.reward.ProgressTracking;
import com.example.readingrewards.domain.model.reward.RewardTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProgressTrackingPolicyService {

    public void validateTrackingConsistency(ProgressTracking tracking) {
        if (tracking == null) {
            return;
        }

        if (tracking.getTrackingType() == ProgressTracking.TrackingType.CHAPTERS) {
            Integer total = tracking.getTotalChapters();
            Integer current = tracking.getCurrentChapter();
            if (total == null || total < 1) {
                throw new IllegalArgumentException("Chapter count required to complete this book");
            }
            if (current != null && current < 0) {
                throw new IllegalArgumentException("Current chapter cannot be negative");
            }
            if (current != null && current > total) {
                throw new IllegalArgumentException("Current chapter cannot exceed total chapters");
            }
        }

        if (tracking.getTrackingType() == ProgressTracking.TrackingType.PAGES) {
            Integer total = tracking.getTotalPages();
            Integer current = tracking.getCurrentPage();
            if (total != null && total < 1) {
                throw new IllegalArgumentException("Total pages must be greater than zero");
            }
            if (current != null && current < 0) {
                throw new IllegalArgumentException("Current page cannot be negative");
            }
            if (total != null && current != null && current > total) {
                throw new IllegalArgumentException("Current page cannot exceed total pages");
            }
        }
    }

    public void validateCompletionAllowed(RewardTemplate.RewardUnit unit, ProgressTracking tracking) {
        if (unit != RewardTemplate.RewardUnit.PER_CHAPTER) {
            return;
        }

        if (tracking == null || tracking.getTrackingType() != ProgressTracking.TrackingType.CHAPTERS) {
            throw new IllegalArgumentException("Chapter count required to complete this book");
        }

        Integer total = tracking.getTotalChapters();
        if (total == null || total < 1) {
            throw new IllegalArgumentException("Chapter count required to complete this book");
        }
    }

    public boolean canComplete(RewardTemplate.RewardUnit unit, ProgressTracking tracking) {
        try {
            validateCompletionAllowed(unit, tracking);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
