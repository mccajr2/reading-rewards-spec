package com.example.readingrewards.domain.model.reward;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reward_templates")
public class RewardTemplate {

    public enum RewardScope {
        FAMILY,
        PER_CHILD
    }

    public enum RewardType {
        MONEY,
        TIME,
        CUSTOM_TEXT
    }

    public enum RewardUnit {
        PER_CHAPTER,
        PER_BOOK
    }

    public enum RewardFrequency {
        IMMEDIATE,
        ON_COMPLETION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "child_id")
    private UUID childId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false)
    private RewardType rewardType = RewardType.MONEY;

    @Column(nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardUnit unit = RewardUnit.PER_CHAPTER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardFrequency frequency = RewardFrequency.IMMEDIATE;

    @Column(name = "canned_template_id")
    private String cannedTemplateId;

    @Column(nullable = false)
    private String description = "";

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void validate() {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Reward amount must be >= 0");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Reward description is required");
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
    public UUID getChildId() { return childId; }
    public void setChildId(UUID childId) { this.childId = childId; }
    public RewardType getRewardType() { return rewardType; }
    public void setRewardType(RewardType rewardType) { this.rewardType = rewardType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public RewardUnit getUnit() { return unit; }
    public void setUnit(RewardUnit unit) { this.unit = unit; }
    public RewardFrequency getFrequency() { return frequency; }
    public void setFrequency(RewardFrequency frequency) { this.frequency = frequency; }
    public String getCannedTemplateId() { return cannedTemplateId; }
    public void setCannedTemplateId(String cannedTemplateId) { this.cannedTemplateId = cannedTemplateId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
