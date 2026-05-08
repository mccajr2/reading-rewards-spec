package com.example.readingrewards.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chapter_reads")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChapterRead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "book_read_id", nullable = false)
    private UUID bookReadId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_read_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"book", "user"})
    private BookRead bookRead;

    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"book"})
    private Chapter chapter;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "completion_date", nullable = false)
    private LocalDateTime completionDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // --- accessors ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getBookReadId() { return bookReadId; }
    public void setBookReadId(UUID bookReadId) { this.bookReadId = bookReadId; }

    public BookRead getBookRead() { return bookRead; }

    public UUID getChapterId() { return chapterId; }
    public void setChapterId(UUID chapterId) { this.chapterId = chapterId; }

    public Chapter getChapter() { return chapter; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public LocalDateTime getCompletionDate() { return completionDate; }
    public void setCompletionDate(LocalDateTime completionDate) { this.completionDate = completionDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
