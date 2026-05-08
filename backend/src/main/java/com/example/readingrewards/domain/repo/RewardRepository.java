package com.example.readingrewards.domain.repo;

import com.example.readingrewards.domain.model.Reward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface RewardRepository extends JpaRepository<Reward, UUID> {
    List<Reward> findByUserId(UUID userId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Reward r WHERE r.userId = :userId AND r.type = 'EARN'")
    Double getTotalEarnedByUserId(UUID userId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Reward r WHERE r.userId = :userId AND r.type = 'PAYOUT'")
    Double getTotalPaidOutByUserId(UUID userId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Reward r WHERE r.userId = :userId AND r.type = 'SPEND'")
    Double getTotalSpentByUserId(UUID userId);
}
