package com.example.readingrewards.domain.service.reward;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.domain.model.reward.RewardTemplate;
import com.example.readingrewards.domain.repo.reward.RewardTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class PerChildRewardService {

    private final RewardTemplateRepository rewardTemplateRepository;
    private final UserRepository userRepository;

    public PerChildRewardService(
        RewardTemplateRepository rewardTemplateRepository,
        UserRepository userRepository
    ) {
        this.rewardTemplateRepository = rewardTemplateRepository;
        this.userRepository = userRepository;
    }

    public RewardTemplate createPerChildReward(UUID parentId, UUID childId, RewardTemplate draft) {
        Objects.requireNonNull(childId, "childId is required");
        User child = userRepository.findById(childId)
            .orElseThrow(() -> new IllegalArgumentException("Child not found"));

        if (child.getRole() != User.UserRole.CHILD || !parentId.equals(child.getParentId())) {
            throw new IllegalArgumentException("Not authorized to configure rewards for this child");
        }

        draft.setId(null);
        draft.setParentId(parentId);
        draft.setChildId(childId);
        draft.setDeleted(false);
        draft.validate();
        return rewardTemplateRepository.save(draft);
    }

    public List<RewardTemplate> listPerChildRewards(UUID parentId, UUID childId) {
        return rewardTemplateRepository.findByParentIdAndChildIdAndDeletedFalse(parentId, childId);
    }
}
