package com.example.readingrewards.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "books")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Book {

    @Id
    @Column(name = "google_book_id", length = 50)
    private String googleBookId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    // Stored as comma-separated in DB; exposed as List<String> via getters
    @Column(name = "authors", nullable = false, columnDefinition = "TEXT")
    private String authorsString;

    @Transient
    private List<String> authors;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- accessors ---

    public String getGoogleBookId() { return googleBookId; }
    public void setGoogleBookId(String googleBookId) { this.googleBookId = googleBookId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public List<String> getAuthors() {
        if (authors == null && authorsString != null && !authorsString.isEmpty()) {
            authors = Arrays.stream(authorsString.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
        this.authorsString = (authors != null) ? String.join(", ", authors) : null;
    }

    @PrePersist
    @PreUpdate
    private void syncAuthorsToString() {
        if (authors != null) {
            authorsString = String.join(", ", authors);
        }
    }

    @PostLoad
    private void syncAuthorsFromString() {
        if (authorsString != null && !authorsString.isEmpty()) {
            authors = Arrays.stream(authorsString.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }
    }
}
