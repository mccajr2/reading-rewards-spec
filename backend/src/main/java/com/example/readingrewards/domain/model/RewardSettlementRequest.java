package com.example.readingrewards.domain.model;

import com.example.readingrewards.auth.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reward_settlement_requests")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RewardSettlementRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "child_user_id", nullable = false)
    private UUID childUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_user_id", insertable = false, updatable = false)
    @JsonIgnore
    private User childUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 10)
    private SettlementRequestType requestType;

    @Column(name = "requested_amount", nullable = false)
    private Double requestedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private SettlementRequestStatus status = SettlementRequestStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by_parent_id")
    private UUID resolvedByParentId;

    @Column(name = "reward_option_id")
    private UUID rewardOptionId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getChildUserId() { return childUserId; }
    public void setChildUserId(UUID childUserId) { this.childUserId = childUserId; }

    public User getChildUser() { return childUser; }

    public SettlementRequestType getRequestType() { return requestType; }
    public void setRequestType(SettlementRequestType requestType) { this.requestType = requestType; }

    public Double getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(Double requestedAmount) { this.requestedAmount = requestedAmount; }

    public SettlementRequestStatus getStatus() { return status; }
    public void setStatus(SettlementRequestStatus status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getRequestedAt() { return requestedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public UUID getResolvedByParentId() { return resolvedByParentId; }
    public void setResolvedByParentId(UUID resolvedByParentId) { this.resolvedByParentId = resolvedByParentId; }

    public UUID getRewardOptionId() { return rewardOptionId; }
    public void setRewardOptionId(UUID rewardOptionId) { this.rewardOptionId = rewardOptionId; }
}
