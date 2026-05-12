package com.example.readingrewards.domain.model.reward;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "progress_tracking")
public class ProgressTracking {

    public enum TrackingType {
        CHAPTERS,
        PAGES,
        NONE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "book_read_id", nullable = false)
    private UUID bookReadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tracking_type", nullable = false)
    private TrackingType trackingType = TrackingType.NONE;

    @Column(name = "total_chapters")
    private Integer totalChapters;

    @Column(name = "current_chapter")
    private Integer currentChapter;

    @Column(name = "chapters_read_list", length = 2000)
    private String chaptersReadList;

    @Column(name = "total_pages")
    private Integer totalPages;

    @Column(name = "current_page")
    private Integer currentPage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBookReadId() { return bookReadId; }
    public void setBookReadId(UUID bookReadId) { this.bookReadId = bookReadId; }
    public TrackingType getTrackingType() { return trackingType; }
    public void setTrackingType(TrackingType trackingType) { this.trackingType = trackingType; }
    public Integer getTotalChapters() { return totalChapters; }
    public void setTotalChapters(Integer totalChapters) { this.totalChapters = totalChapters; }
    public Integer getCurrentChapter() { return currentChapter; }
    public void setCurrentChapter(Integer currentChapter) { this.currentChapter = currentChapter; }
    public String getChaptersReadList() { return chaptersReadList; }
    public void setChaptersReadList(String chaptersReadList) { this.chaptersReadList = chaptersReadList; }
    public Integer getTotalPages() { return totalPages; }
    public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }
    public Integer getCurrentPage() { return currentPage; }
    public void setCurrentPage(Integer currentPage) { this.currentPage = currentPage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
