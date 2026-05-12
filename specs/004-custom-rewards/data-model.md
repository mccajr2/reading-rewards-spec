# Data Model: Parent Reward Customization

**Phase**: 1 | **Feature**: `004-custom-rewards` | **Date**: 2026-05-12

## Entities

### 1. RewardOption

Represents a parent-defined earning rule offered to one or more children.

**Fields**:
- `id` (UUID)
- `familyId` (UUID)
- `scopeType` (`FAMILY` | `CHILD`)
- `scopeChildId` (UUID, nullable when `scopeType=FAMILY`)
- `name` (string, 1..120)
- `description` (string, 0..500)
- `unitType` (`MONEY_USD` | `SCREEN_MINUTES` | `NINTENDO_MINUTES` | `CUSTOM_UNIT`)
- `earningBasis` (`PER_CHAPTER` | `PER_BOOK` | `PER_PAGE_MILESTONE`)
- `payoutValue` (decimal or integer by unit, >0)
- `pageMilestoneSize` (integer, nullable unless `PER_PAGE_MILESTONE`, >=1)
- `isActive` (boolean)
- `createdByParentId` (UUID)
- `createdAt` (timestamp)
- `updatedAt` (timestamp)

**Validation rules**:
- `scopeChildId` required when `scopeType=CHILD`.
- `pageMilestoneSize` required only for `PER_PAGE_MILESTONE`.
- `payoutValue` cannot be zero or negative.
- Duplicate active options with same name for same child are discouraged and flagged in UI.

---

### 2. ChildRewardSelection

Tracks which offered option a child has currently selected for future earnings.

**Fields**:
- `id` (UUID)
- `childId` (UUID)
- `rewardOptionId` (UUID)
- `effectiveFrom` (timestamp)
- `effectiveTo` (timestamp, nullable)
- `selectedBy` (`CHILD` | `PARENT`)

**Validation rules**:
- Only one active selection per child at a time.
- `rewardOptionId` must be visible to child via additive scope resolution.

**State transitions**:
- `Active` -> `Replaced` when child selects a different option.

---

### 3. ReadingAssignment

Represents a child-book tracking context and chosen tracking mode.

**Fields**:
- `id` (UUID)
- `childId` (UUID)
- `bookId` (string or UUID)
- `trackingMode` (`BOOK_ONLY` | `CHAPTERS` | `PAGES`)
- `chapterCountPlanned` (integer, nullable)
- `chaptersCompleted` (integer, default 0)
- `totalPages` (integer, nullable)
- `currentPage` (integer, nullable)
- `isCompleted` (boolean)
- `metadataSource` (`OPEN_LIBRARY` | `MANUAL` | `NONE`)
- `suggestedPageCount` (integer, nullable)
- `manualPageOverride` (integer, nullable)

**Validation rules**:
- `BOOK_ONLY` does not require chapter/page fields.
- `CHAPTERS` requires `chapterCountPlanned >= 1`.
- `PAGES` requires `totalPages >= 1` and `currentPage <= totalPages`.
- Manual override wins over metadata suggestion.

**State transitions**:
- `InProgress` -> `Completed` when marked complete.
- Progress updates are monotonic unless parent adjusts records.

---

### 4. RewardLedger

Unit-scoped running balance container for each child.

**Fields**:
- `id` (UUID)
- `childId` (UUID)
- `unitType` (same enum family as `RewardOption.unitType`)
- `availableBalance` (numeric)
- `pendingPayoutBalance` (numeric)
- `updatedAt` (timestamp)

**Validation rules**:
- One active ledger per `(childId, unitType)` pair.
- No cross-unit aggregation.

---

### 5. RewardLedgerEntry

Immutable transaction entry for earnings and settlements.

**Fields**:
- `id` (UUID)
- `ledgerId` (UUID)
- `entryType` (`EARNED` | `PAYOUT_REQUESTED` | `PAID_OUT` | `SPEND_REQUESTED` | `SPENT` | `ADJUSTMENT`)
- `amount` (numeric, signed by type policy)
- `sourceReadingAssignmentId` (UUID, nullable)
- `sourceProgressEventId` (UUID, nullable)
- `notes` (string, nullable)
- `createdBy` (`SYSTEM` | `CHILD` | `PARENT`)
- `createdAt` (timestamp)

**Validation rules**:
- `PAID_OUT` and `SPENT` require parent approval linkage.
- Entry must match ledger unit semantics.

---

### 6. RewardSettlementRequest

Child-initiated request awaiting parent action.

**Fields**:
- `id` (UUID)
- `childId` (UUID)
- `ledgerId` (UUID)
- `requestType` (`PAYOUT` | `SPEND`)
- `requestedAmount` (numeric)
- `status` (`PENDING` | `APPROVED` | `REJECTED` | `CANCELLED`)
- `requestedAt` (timestamp)
- `resolvedAt` (timestamp, nullable)
- `resolvedByParentId` (UUID, nullable)

**Validation rules**:
- Child can create only against available/pending balances allowed by policy.
- Only parent can approve/reject.

**State transitions**:
- `PENDING` -> `APPROVED` finalizes corresponding ledger entry.
- `PENDING` -> `REJECTED` leaves balance unchanged.

---

### 7. FamilyMessage

In-app communication between parent and child for reward follow-up.

**Fields**:
- `id` (UUID)
- `familyId` (UUID)
- `senderRole` (`PARENT` | `CHILD`)
- `senderUserId` (UUID)
- `recipientUserId` (UUID)
- `messageType` (`NUDGE` | `ENCOURAGEMENT`)
- `body` (string, 1..500)
- `linkedSettlementRequestId` (UUID, nullable)
- `createdAt` (timestamp)
- `emailNotificationSent` (boolean)

**Validation rules**:
- `NUDGE` from child to parent only.
- `ENCOURAGEMENT` from parent to child only.
- For `NUDGE`, enforce one message per child per rolling 24 hours.
- Email notifications enabled for `NUDGE` only.

---

## Relationships

- `RewardOption (1) -> (0..*) ChildRewardSelection`
- `Child (1) -> (0..*) ReadingAssignment`
- `Child (1) -> (0..*) RewardLedger` by `unitType`
- `RewardLedger (1) -> (0..*) RewardLedgerEntry`
- `RewardLedger (1) -> (0..*) RewardSettlementRequest`
- `RewardSettlementRequest (0..1) -> (0..*) FamilyMessage`

## Derived Rules

- Child-visible reward options = active family options + active child-scoped options for that child.
- Page milestone earnings: `floor(cumulativePages / milestoneSize)` determines total milestone count earned; delta milestones since previous event generate new `EARNED` entries.
- Parent accounts never participate as earning subjects or reward selectors.
