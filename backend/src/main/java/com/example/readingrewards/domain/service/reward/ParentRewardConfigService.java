package com.example.readingrewards.domain.service.reward;

import com.example.readingrewards.domain.model.reward.RewardTemplate;
import com.example.readingrewards.domain.repo.reward.RewardTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ParentRewardConfigService {

    private final RewardTemplateRepository rewardTemplateRepository;

    public ParentRewardConfigService(RewardTemplateRepository rewardTemplateRepository) {
        this.rewardTemplateRepository = rewardTemplateRepository;
    }

    public List<RewardTemplate> listFamilyRewards(UUID parentId) {
        return rewardTemplateRepository.findByParentIdAndDeletedFalse(parentId);
    }

    public RewardTemplate createFamilyReward(UUID parentId, RewardTemplate draft) {
        draft.setId(null);
        draft.setParentId(parentId);
        draft.setChildId(null);
        draft.setDeleted(false);
        draft.validate();
        return rewardTemplateRepository.save(draft);
    }

    public RewardTemplate updateFamilyReward(UUID parentId, UUID rewardTemplateId, RewardTemplate update) {
        Objects.requireNonNull(rewardTemplateId, "rewardTemplateId is required");
        RewardTemplate existing = rewardTemplateRepository.findById(rewardTemplateId)
            .orElseThrow(() -> new IllegalArgumentException("Reward template not found"));

        if (!parentId.equals(existing.getParentId())) {
            throw new IllegalArgumentException("Not authorized to update this reward template");
        }

        existing.setRewardType(update.getRewardType());
        existing.setAmount(update.getAmount());
        existing.setUnit(update.getUnit());
        existing.setFrequency(update.getFrequency());
        existing.setCannedTemplateId(update.getCannedTemplateId());
        existing.setDescription(update.getDescription());
        existing.validate();
        return rewardTemplateRepository.save(existing);
    }
}
