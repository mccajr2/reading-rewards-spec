package com.example.readingrewards.domain.repo;

import com.example.readingrewards.domain.model.BookRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookReadRepository extends JpaRepository<BookRead, UUID> {
    List<BookRead> findByUserId(UUID userId);
    Optional<BookRead> findByUserIdAndGoogleBookIdAndEndDateIsNull(UUID userId, String googleBookId);
}
