package com.example.readingrewards.domain.repo;

import com.example.readingrewards.domain.model.RewardSettlementRequest;
import com.example.readingrewards.domain.model.SettlementRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RewardSettlementRequestRepository extends JpaRepository<RewardSettlementRequest, UUID> {

    List<RewardSettlementRequest> findByChildUserIdOrderByRequestedAtDesc(UUID childUserId);

    List<RewardSettlementRequest> findByChildUserIdAndStatusOrderByRequestedAtDesc(
            UUID childUserId, SettlementRequestStatus status);
}
