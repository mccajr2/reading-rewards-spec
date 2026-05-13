package com.example.readingrewards.domain.dto;

import com.example.readingrewards.domain.model.Book;
import com.example.readingrewards.domain.model.BookRead;
import com.example.readingrewards.domain.model.RewardEarningBasis;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class BookReadProgressDto {
    private UUID bookReadId;
    private String googleBookId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private List<String> authors;
    private LocalDateTime startDate;
    private int readCount;
    private List<UUID> readChapterIds;
    private RewardEarningBasis bookEarningBasis;

    public BookReadProgressDto(BookRead br, Book book, int readCount, List<UUID> readChapterIds) {
        this.bookReadId = br.getId();
        this.googleBookId = book.getGoogleBookId();
        this.title = book.getTitle();
        this.description = book.getDescription();
        this.thumbnailUrl = book.getThumbnailUrl();
        this.authors = book.getAuthors();
        this.startDate = br.getStartDate();
        this.readCount = readCount;
        this.readChapterIds = readChapterIds;
        this.bookEarningBasis = br.getBookEarningBasis();
    }

    public UUID getBookReadId() { return bookReadId; }
    public String getGoogleBookId() { return googleBookId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public List<String> getAuthors() { return authors; }
    public LocalDateTime getStartDate() { return startDate; }
    public int getReadCount() { return readCount; }
    public List<UUID> getReadChapterIds() { return readChapterIds; }
    public RewardEarningBasis getBookEarningBasis() { return bookEarningBasis; }
}
