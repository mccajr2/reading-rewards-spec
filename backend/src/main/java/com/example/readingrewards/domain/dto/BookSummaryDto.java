package com.example.readingrewards.domain.dto;

import java.util.List;

public class BookSummaryDto {
    private String googleBookId;
    private String title;
    private List<String> authors;
    private String description;
    private String thumbnailUrl;

    public BookSummaryDto() {}

    public BookSummaryDto(String googleBookId, String title, List<String> authors,
                          String description, String thumbnailUrl) {
        this.googleBookId = googleBookId;
        this.title = title;
        this.authors = authors;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getGoogleBookId() { return googleBookId; }
    public void setGoogleBookId(String googleBookId) { this.googleBookId = googleBookId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getAuthors() { return authors; }
    public void setAuthors(List<String> authors) { this.authors = authors; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
}
