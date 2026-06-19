package com.example.readingrewards.domain.model;

/**
 * Represents the reading progress tracking mode for a book.
 * 
 * BOOK_ONLY: Track only book completion (no chapter or page tracking)
 * CHAPTERS: Track progress by chapters (chapters_completed)
 * PAGES: Track progress by pages (current_page, with page_milestone_carry_forward for milestone-based earning)
 */
public enum ReadingTrackingMode {
    BOOK_ONLY,
    CHAPTERS,
    PAGES
}
