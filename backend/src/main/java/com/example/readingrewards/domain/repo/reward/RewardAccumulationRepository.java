package com.example.readingrewards.domain.repo.reward;

import com.example.readingrewards.domain.model.reward.RewardAccumulation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RewardAccumulationRepository extends JpaRepository<RewardAccumulation, UUID> {
    List<RewardAccumulation> findByChildIdOrderByCreatedAtDesc(UUID childId);
    List<RewardAccumulation> findByChildIdAndStatus(UUID childId, RewardAccumulation.AccumulationStatus status);
}
