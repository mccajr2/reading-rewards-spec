package com.example.readingrewards.domain.repo;

import com.example.readingrewards.domain.model.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ChapterRepository extends JpaRepository<Chapter, UUID> {
    List<Chapter> findByGoogleBookIdOrderByChapterIndex(String googleBookId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Chapter c WHERE c.googleBookId = :googleBookId")
    void deleteByGoogleBookId(String googleBookId);
}
