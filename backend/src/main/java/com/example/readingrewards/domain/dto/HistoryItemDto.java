package com.example.readingrewards.domain.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class HistoryItemDto {
    private UUID id;
    private UUID chapterId;
    private String chapterName;
    private String bookTitle;
    private LocalDateTime completionDate;

    public HistoryItemDto(UUID id, UUID chapterId, String chapterName, String bookTitle, LocalDateTime completionDate) {
        this.id = id;
        this.chapterId = chapterId;
        this.chapterName = chapterName;
        this.bookTitle = bookTitle;
        this.completionDate = completionDate;
    }

    public UUID getId() { return id; }
    public UUID getChapterId() { return chapterId; }
    public String getChapterName() { return chapterName; }
    public String getBookTitle() { return bookTitle; }
    public LocalDateTime getCompletionDate() { return completionDate; }
}
