package com.example.readingrewards.domain.repo.reward;

import com.example.readingrewards.domain.model.reward.RewardAccumulation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface RewardAccumulationRepository extends JpaRepository<RewardAccumulation, UUID> {
    List<RewardAccumulation> findByChildIdOrderByCreatedAtDesc(UUID childId);
    List<RewardAccumulation> findByChildIdAndStatus(UUID childId, RewardAccumulation.AccumulationStatus status);
    List<RewardAccumulation> findByChildIdAndStatusOrderByCreatedAtDesc(UUID childId, RewardAccumulation.AccumulationStatus status);
    List<RewardAccumulation> findByBookReadIdOrderByCreatedAtDesc(UUID bookReadId);

    @Query("SELECT COALESCE(SUM(r.amountEarned), 0) FROM RewardAccumulation r WHERE r.childId = :childId AND r.status = :status")
    BigDecimal sumAmountByChildId(UUID childId, RewardAccumulation.AccumulationStatus status);
}
