package com.example.readingrewards.domain.model.reward;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reward_accumulations")
public class RewardAccumulation {

    public enum AccumulationStatus {
        EARNED,
        PENDING_PAYOUT,
        PAID
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "book_read_id", nullable = false)
    private UUID bookReadId;

    @Column(name = "reward_template_id", nullable = false)
    private UUID rewardTemplateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false)
    private RewardTemplate.RewardType rewardType = RewardTemplate.RewardType.MONEY;

    @Column(name = "amount_earned", nullable = false)
    private BigDecimal amountEarned = BigDecimal.ZERO;

    @Column(name = "unit_count", nullable = false)
    private Integer unitCount = 0;

    @Column(name = "calculation_note")
    private String calculationNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccumulationStatus status = AccumulationStatus.EARNED;

    @Column(name = "payout_date")
    private LocalDateTime payoutDate;

    @Column(name = "related_message_id")
    private UUID relatedMessageId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getChildId() { return childId; }
    public void setChildId(UUID childId) { this.childId = childId; }
    public UUID getBookReadId() { return bookReadId; }
    public void setBookReadId(UUID bookReadId) { this.bookReadId = bookReadId; }
    public UUID getRewardTemplateId() { return rewardTemplateId; }
    public void setRewardTemplateId(UUID rewardTemplateId) { this.rewardTemplateId = rewardTemplateId; }
    public RewardTemplate.RewardType getRewardType() { return rewardType; }
    public void setRewardType(RewardTemplate.RewardType rewardType) { this.rewardType = rewardType; }
    public BigDecimal getAmountEarned() { return amountEarned; }
    public void setAmountEarned(BigDecimal amountEarned) { this.amountEarned = amountEarned; }
    public Integer getUnitCount() { return unitCount; }
    public void setUnitCount(Integer unitCount) { this.unitCount = unitCount; }
    public String getCalculationNote() { return calculationNote; }
    public void setCalculationNote(String calculationNote) { this.calculationNote = calculationNote; }
    public AccumulationStatus getStatus() { return status; }
    public void setStatus(AccumulationStatus status) { this.status = status; }
    public LocalDateTime getPayoutDate() { return payoutDate; }
    public void setPayoutDate(LocalDateTime payoutDate) { this.payoutDate = payoutDate; }
    public UUID getRelatedMessageId() { return relatedMessageId; }
    public void setRelatedMessageId(UUID relatedMessageId) { this.relatedMessageId = relatedMessageId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
