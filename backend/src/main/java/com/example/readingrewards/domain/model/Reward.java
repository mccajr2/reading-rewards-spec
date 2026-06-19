package com.example.readingrewards.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rewards")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RewardType type;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Double amount = 0.0;

    @Column(name = "chapter_read_id")
    private UUID chapterReadId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_read_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"bookRead", "chapter"})
    private ChapterRead chapterRead;

    @Column(name = "reward_option_id")
    private UUID rewardOptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_option_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"ownerUser", "childUser"})
    private RewardOption rewardOption;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // --- accessors ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public RewardType getType() { return type; }
    public void setType(RewardType type) { this.type = type; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public UUID getChapterReadId() { return chapterReadId; }
    public void setChapterReadId(UUID chapterReadId) { this.chapterReadId = chapterReadId; }

    public ChapterRead getChapterRead() { return chapterRead; }

    public UUID getRewardOptionId() { return rewardOptionId; }
    public void setRewardOptionId(UUID rewardOptionId) { this.rewardOptionId = rewardOptionId; }

    public RewardOption getRewardOption() { return rewardOption; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
