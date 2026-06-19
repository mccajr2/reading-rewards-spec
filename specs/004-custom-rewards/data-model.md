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
- `valueType` (`MONEY` | `NON_MONEY`)
- `currencyCode` (string, nullable unless `valueType=MONEY`, default `USD`)
- `moneyAmount` (decimal, nullable unless `valueType=MONEY`, >0)
- `nonMoneyQuantity` (decimal, nullable unless `valueType=NON_MONEY`, >0)
- `nonMoneyUnitLabel` (string, nullable unless `valueType=NON_MONEY`, 1..40)
- `earningBasis` (`PER_CHAPTER` | `PER_BOOK` | `PER_PAGE_MILESTONE`)
- `pageMilestoneSize` (integer, nullable unless `PER_PAGE_MILESTONE`, >=1)
- `isActive` (boolean)
- `createdByParentId` (UUID)
- `createdAt` (timestamp)
- `updatedAt` (timestamp)

**Validation rules**:
- `scopeChildId` required when `scopeType=CHILD`.
- `pageMilestoneSize` required only for `PER_PAGE_MILESTONE`.
- For `MONEY`, `moneyAmount` is required and `nonMoney*` fields are null.
- For `NON_MONEY`, `nonMoneyQuantity` and `nonMoneyUnitLabel` are required and `moneyAmount` is null.
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
- `bookEarningBasis` (`PER_CHAPTER` | `PER_BOOK` | `PER_PAGE_MILESTONE`)
- `basisLockedAt` (timestamp)
- `chapterCountPlanned` (integer, nullable)
- `chaptersCompleted` (integer, default 0)
- `totalPages` (integer, nullable)
- `pageCountConfirmed` (boolean, default false for PER_PAGE until user confirm/override)
- `currentPage` (integer, nullable)
- `isCompleted` (boolean)
- `metadataSource` (`OPEN_LIBRARY` | `MANUAL` | `NONE`)
- `suggestedPageCount` (integer, nullable)
- `manualPageOverride` (integer, nullable)

**Validation rules**:
- `BOOK_ONLY` does not require chapter/page fields.
- `bookEarningBasis` is required at assignment start and immutable while assignment is in progress.
- `CHAPTERS` requires `chapterCountPlanned >= 1`.
- `PAGES` requires `totalPages >= 1` and `currentPage <= totalPages`.
- For `PER_PAGE`, earnings are blocked until `pageCountConfirmed=true`.
- For `PER_BOOK`, `chapterCountPlanned` and page fields are optional tracking-only and do not gate earning eligibility.
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
- `valueType` (`MONEY` | `NON_MONEY`)
- `unitKey` (string; `USD` for money or normalized non-money unit label)
- `availableBalance` (numeric)
- `pendingPayoutBalance` (numeric)
- `updatedAt` (timestamp)

**Validation rules**:
- One active ledger per `(childId, valueType, unitKey)` pair.
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
- If multiple eligible options exist for chapter/page completion, completion cannot finalize until child selects one eligible option for that event.
- Global child active selection is a fallback default only and is superseded by explicit per-book basis and completion-time option choice.
- Once `bookEarningBasis` is set for a reading assignment, it cannot be changed until the assignment is completed.
- For PER_PAGE basis, OpenLibrary page count prefill is provisional and must be user confirmed or overridden before milestone earnings begin.
- For PER_BOOK basis, completion event alone is sufficient for eligibility regardless of chapter/page tracking fields.

---

### 8. ProgressCompletionEventSelection

Represents the child's explicit option choice for a specific chapter/page completion event when multiple options are eligible.

**Fields**:
- `id` (UUID)
- `childId` (UUID)
- `readingAssignmentId` (UUID)
- `progressEventType` (`CHAPTER_COMPLETION` | `PAGE_MILESTONE_COMPLETION`)
- `progressEventRefId` (UUID or deterministic event key)
- `selectedRewardOptionId` (UUID)
- `selectedAt` (timestamp)

**Validation rules**:
- Required only when more than one eligible option exists for the event.
- `selectedRewardOptionId` must belong to event-eligible options and match assignment `bookEarningBasis`.

**State transitions**:
- `PendingSelection` -> `Selected` -> `EarningPosted`
- Parent accounts never participate as earning subjects or reward selectors.
