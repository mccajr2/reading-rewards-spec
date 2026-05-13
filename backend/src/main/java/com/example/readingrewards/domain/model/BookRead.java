package com.example.readingrewards.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "book_reads")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BookRead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "google_book_id", nullable = false, length = 50)
    private String googleBookId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "google_book_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"chapters"})
    private Book book;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_earning_basis", length = 30)
    private RewardEarningBasis bookEarningBasis;

    @Column(name = "basis_locked_at")
    private LocalDateTime basisLockedAt;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "page_count_confirmed", nullable = false)
    private Boolean pageCountConfirmed = Boolean.FALSE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Derived: true when endDate is null. Not persisted as a column. */
    public boolean isInProgress() {
        return endDate == null;
    }

    // --- accessors ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getGoogleBookId() { return googleBookId; }
    public void setGoogleBookId(String googleBookId) { this.googleBookId = googleBookId; }

    public Book getBook() { return book; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public RewardEarningBasis getBookEarningBasis() { return bookEarningBasis; }
    public void setBookEarningBasis(RewardEarningBasis bookEarningBasis) { this.bookEarningBasis = bookEarningBasis; }

    public LocalDateTime getBasisLockedAt() { return basisLockedAt; }
    public void setBasisLockedAt(LocalDateTime basisLockedAt) { this.basisLockedAt = basisLockedAt; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public Boolean getPageCountConfirmed() { return pageCountConfirmed; }
    public void setPageCountConfirmed(Boolean pageCountConfirmed) { this.pageCountConfirmed = pageCountConfirmed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
