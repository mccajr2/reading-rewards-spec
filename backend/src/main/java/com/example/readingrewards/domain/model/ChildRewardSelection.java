package com.example.readingrewards.domain.model;

import com.example.readingrewards.auth.model.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "child_reward_selections")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChildRewardSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "child_user_id", nullable = false)
    private UUID childUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_user_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"parent", "children", "password"})
    private User childUser;

    @Column(name = "reward_option_id", nullable = false)
    private UUID rewardOptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_option_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"ownerUser", "childUser"})
    private RewardOption rewardOption;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "selected_at", nullable = false, updatable = false)
    private LocalDateTime selectedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getChildUserId() { return childUserId; }
    public void setChildUserId(UUID childUserId) { this.childUserId = childUserId; }

    public User getChildUser() { return childUser; }

    public UUID getRewardOptionId() { return rewardOptionId; }
    public void setRewardOptionId(UUID rewardOptionId) { this.rewardOptionId = rewardOptionId; }

    public RewardOption getRewardOption() { return rewardOption; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getSelectedAt() { return selectedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}