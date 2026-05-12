package com.example.readingrewards.domain.model.reward;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reward_selections")
public class RewardSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "book_read_id", nullable = false)
    private UUID bookReadId;

    @Column(name = "reward_template_id", nullable = false)
    private UUID rewardTemplateId;

    @Column(name = "locked_amount", nullable = false)
    private BigDecimal lockedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "locked_unit", nullable = false)
    private RewardTemplate.RewardUnit lockedUnit = RewardTemplate.RewardUnit.PER_CHAPTER;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "selected_at", nullable = false, updatable = false)
    private LocalDateTime selectedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getChildId() { return childId; }
    public void setChildId(UUID childId) { this.childId = childId; }
    public UUID getBookReadId() { return bookReadId; }
    public void setBookReadId(UUID bookReadId) { this.bookReadId = bookReadId; }
    public UUID getRewardTemplateId() { return rewardTemplateId; }
    public void setRewardTemplateId(UUID rewardTemplateId) { this.rewardTemplateId = rewardTemplateId; }
    public BigDecimal getLockedAmount() { return lockedAmount; }
    public void setLockedAmount(BigDecimal lockedAmount) { this.lockedAmount = lockedAmount; }
    public RewardTemplate.RewardUnit getLockedUnit() { return lockedUnit; }
    public void setLockedUnit(RewardTemplate.RewardUnit lockedUnit) { this.lockedUnit = lockedUnit; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getSelectedAt() { return selectedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
