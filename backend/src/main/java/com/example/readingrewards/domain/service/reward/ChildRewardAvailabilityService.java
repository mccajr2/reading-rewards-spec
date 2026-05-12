package com.example.readingrewards.domain.service.reward;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.domain.model.reward.RewardTemplate;
import com.example.readingrewards.domain.repo.reward.RewardTemplateRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ChildRewardAvailabilityService {

    private final RewardTemplateRepository rewardTemplateRepository;

    public ChildRewardAvailabilityService(RewardTemplateRepository rewardTemplateRepository) {
        this.rewardTemplateRepository = rewardTemplateRepository;
    }

    public List<RewardTemplate> listAvailableRewards(User child) {
        Objects.requireNonNull(child, "child is required");
        Objects.requireNonNull(child.getId(), "child id is required");

        UUID parentId = child.getParentId();
        if (parentId == null) {
            return List.of();
        }

        List<RewardTemplate> family = rewardTemplateRepository.findByParentIdAndDeletedFalse(parentId);
        List<RewardTemplate> perChild = rewardTemplateRepository.findByParentIdAndChildIdAndDeletedFalse(parentId, child.getId());

        List<RewardTemplate> available = new ArrayList<>();
        available.addAll(family);
        available.addAll(perChild);

        if (available.isEmpty()) {
            available.add(ensureDefaultTemplate(parentId));
        }

        return available;
    }

    private RewardTemplate ensureDefaultTemplate(UUID parentId) {
        List<RewardTemplate> family = rewardTemplateRepository.findByParentIdAndDeletedFalse(parentId);
        for (RewardTemplate template : family) {
            if ("DEFAULT_100_PER_CHAPTER".equals(template.getCannedTemplateId())) {
                return template;
            }
        }

        RewardTemplate fallback = new RewardTemplate();
        fallback.setParentId(parentId);
        fallback.setChildId(null);
        fallback.setRewardType(RewardTemplate.RewardType.MONEY);
        fallback.setAmount(BigDecimal.valueOf(1.00));
        fallback.setUnit(RewardTemplate.RewardUnit.PER_CHAPTER);
        fallback.setFrequency(RewardTemplate.RewardFrequency.IMMEDIATE);
        fallback.setCannedTemplateId("DEFAULT_100_PER_CHAPTER");
        fallback.setDescription("$1.00 per chapter");
        fallback.setDeleted(false);
        fallback.validate();
        return rewardTemplateRepository.save(fallback);
    }
}
