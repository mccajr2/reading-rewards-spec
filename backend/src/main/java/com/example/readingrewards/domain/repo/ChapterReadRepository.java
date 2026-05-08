package com.example.readingrewards.domain.repo;

import com.example.readingrewards.domain.model.ChapterRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChapterReadRepository extends JpaRepository<ChapterRead, UUID> {
    List<ChapterRead> findByUserId(UUID userId);
    List<ChapterRead> findByBookReadId(UUID bookReadId);
}
