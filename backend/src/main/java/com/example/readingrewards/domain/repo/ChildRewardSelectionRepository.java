package com.example.readingrewards.domain.repo;

import com.example.readingrewards.domain.model.ChildRewardSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChildRewardSelectionRepository extends JpaRepository<ChildRewardSelection, UUID> {
    Optional<ChildRewardSelection> findByChildUserIdAndActiveTrue(UUID childUserId);

    List<ChildRewardSelection> findByRewardOptionIdAndActiveTrue(UUID rewardOptionId);
}