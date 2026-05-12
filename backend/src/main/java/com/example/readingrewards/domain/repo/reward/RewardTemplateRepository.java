package com.example.readingrewards.domain.repo.reward;

import com.example.readingrewards.domain.model.reward.RewardTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RewardTemplateRepository extends JpaRepository<RewardTemplate, UUID> {
    List<RewardTemplate> findByParentIdAndDeletedFalse(UUID parentId);
    List<RewardTemplate> findByParentIdAndChildIdAndDeletedFalse(UUID parentId, UUID childId);
}
