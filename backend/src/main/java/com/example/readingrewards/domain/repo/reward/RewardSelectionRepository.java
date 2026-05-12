package com.example.readingrewards.domain.repo.reward;

import com.example.readingrewards.domain.model.reward.RewardSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RewardSelectionRepository extends JpaRepository<RewardSelection, UUID> {
    Optional<RewardSelection> findByBookReadIdAndActiveTrue(UUID bookReadId);
    List<RewardSelection> findByChildIdAndActiveTrue(UUID childId);
}
