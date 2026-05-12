package com.example.readingrewards.domain.service.reward;

import com.example.readingrewards.domain.model.reward.RewardTemplate;
import com.example.readingrewards.domain.repo.reward.RewardTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class RewardTemplateArchiveService {

    private final RewardTemplateRepository rewardTemplateRepository;

    public RewardTemplateArchiveService(RewardTemplateRepository rewardTemplateRepository) {
        this.rewardTemplateRepository = rewardTemplateRepository;
    }

    public RewardTemplate archiveFamilyReward(UUID parentId, UUID rewardTemplateId) {
        Objects.requireNonNull(rewardTemplateId, "rewardTemplateId is required");
        RewardTemplate existing = rewardTemplateRepository.findById(rewardTemplateId)
            .orElseThrow(() -> new IllegalArgumentException("Reward template not found"));

        if (!parentId.equals(existing.getParentId())) {
            throw new IllegalArgumentException("Not authorized to archive this reward template");
        }

        existing.setDeleted(true);
        return rewardTemplateRepository.save(existing);
    }
}
