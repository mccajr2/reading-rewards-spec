package com.example.readingrewards.domain.model;

import com.example.readingrewards.auth.model.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reward_options")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RewardOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"parent", "children", "password"})
    private User ownerUser;

    @Column(name = "child_user_id")
    private UUID childUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_user_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"parent", "children", "password"})
    private User childUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 20)
    private RewardScopeType scopeType;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "earning_basis", nullable = false, length = 30)
    private RewardEarningBasis earningBasis;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 10)
    private RewardValueType valueType = RewardValueType.MONEY;

    @Column(name = "currency_code", length = 10)
    private String currencyCode;

    @Column(name = "money_amount")
    private Double moneyAmount;

    @Column(name = "non_money_quantity")
    private Double nonMoneyQuantity;

    @Column(name = "non_money_unit_label", length = 40)
    private String nonMoneyUnitLabel;

    @Column(name = "page_milestone_size")
    private Integer pageMilestoneSize;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UUID ownerUserId) { this.ownerUserId = ownerUserId; }

    public User getOwnerUser() { return ownerUser; }

    public UUID getChildUserId() { return childUserId; }
    public void setChildUserId(UUID childUserId) { this.childUserId = childUserId; }

    public User getChildUser() { return childUser; }

    public RewardScopeType getScopeType() { return scopeType; }
    public void setScopeType(RewardScopeType scopeType) { this.scopeType = scopeType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public RewardEarningBasis getEarningBasis() { return earningBasis; }
    public void setEarningBasis(RewardEarningBasis earningBasis) { this.earningBasis = earningBasis; }

    public RewardValueType getValueType() { return valueType; }
    public void setValueType(RewardValueType valueType) { this.valueType = valueType; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public Double getMoneyAmount() { return moneyAmount; }
    public void setMoneyAmount(Double moneyAmount) { this.moneyAmount = moneyAmount; }

    public Double getNonMoneyQuantity() { return nonMoneyQuantity; }
    public void setNonMoneyQuantity(Double nonMoneyQuantity) { this.nonMoneyQuantity = nonMoneyQuantity; }

    public String getNonMoneyUnitLabel() { return nonMoneyUnitLabel; }
    public void setNonMoneyUnitLabel(String nonMoneyUnitLabel) { this.nonMoneyUnitLabel = nonMoneyUnitLabel; }

    public Integer getPageMilestoneSize() { return pageMilestoneSize; }
    public void setPageMilestoneSize(Integer pageMilestoneSize) { this.pageMilestoneSize = pageMilestoneSize; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    /** Returns the earneable amount for ledger entries: moneyAmount for MONEY, nonMoneyQuantity for NON_MONEY. */
    public Double getEffectiveAmount() {
        return valueType == RewardValueType.NON_MONEY ? nonMoneyQuantity : moneyAmount;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}