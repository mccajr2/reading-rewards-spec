package com.example.readingrewards.domain.repo.reward;

import com.example.readingrewards.domain.model.reward.ProgressTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProgressTrackingRepository extends JpaRepository<ProgressTracking, UUID> {
    Optional<ProgressTracking> findByBookReadId(UUID bookReadId);
}
