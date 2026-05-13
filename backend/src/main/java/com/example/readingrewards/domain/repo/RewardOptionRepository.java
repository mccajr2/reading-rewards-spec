package com.example.readingrewards.domain.repo;

import com.example.readingrewards.domain.model.RewardOption;
import com.example.readingrewards.domain.model.RewardScopeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RewardOptionRepository extends JpaRepository<RewardOption, UUID> {
    List<RewardOption> findByOwnerUserIdOrderByCreatedAtAsc(UUID ownerUserId);

    List<RewardOption> findByOwnerUserIdAndScopeTypeAndActiveTrueOrderByCreatedAtAsc(UUID ownerUserId, RewardScopeType scopeType);

    Optional<RewardOption> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
}