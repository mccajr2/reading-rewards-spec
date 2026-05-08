package com.example.readingrewards.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chapters",
    uniqueConstraints = @UniqueConstraint(columnNames = {"google_book_id", "chapter_index"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "google_book_id", nullable = false, length = 50)
    private String googleBookId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "google_book_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"chapters"})
    @JsonIgnore
    private Book book;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(name = "chapter_index", nullable = false)
    private Integer chapterIndex;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- accessors ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getGoogleBookId() { return googleBookId; }
    public void setGoogleBookId(String googleBookId) { this.googleBookId = googleBookId; }

    public Book getBook() { return book; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getChapterIndex() { return chapterIndex; }
    public void setChapterIndex(Integer chapterIndex) { this.chapterIndex = chapterIndex; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
