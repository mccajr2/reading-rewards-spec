# Data Model: Reward Customization Feature

**Version**: 1.0.0  
**Date**: 2026-05-11  
**Feature Spec**: [spec.md](spec.md)  
**Applicable Research**: [research.md](research.md)

---

## Entity Relationship Diagram

```
User (Parent)
  ├─ 1 → ∞ RewardTemplate (family-level rewards)
  └─ 1 → ∞ Message (receives payout reminders & encouragement)

User (Child)
  ├─ 1 → ∞ RewardSelection (active reward choice per book)
  ├─ 1 → ∞ RewardAccumulation (earned rewards ledger)
  ├─ 1 → ∞ ProgressTracking (chapter/page progress)
  └─ 1 → ∞ Message (receives encouragement & payout confirmations)

RewardTemplate
  ├─ 1 → ∞ RewardSelection (children selecting this reward)
  └─ 1 → ∞ RewardAccumulation (earned amounts)

BookRead
  ├─ 1 → ∞ RewardSelection (which reward applies)
  ├─ 1 → ∞ ProgressTracking (chapter/page progress)
  └─ 1 → ∞ RewardAccumulation (earned at completion)

Message
  ├─ N:1 from User (sender: parent or child)
  └─ N:1 to User (recipient: parent or child)
```

---

## Entity Definitions

### 1. RewardTemplate

**Purpose**: Parent-configured reward option available to children (global or per-child).

**Fields**:

| Field | Type | Nullable | Constraints | Notes |
|-------|------|----------|-------------|-------|
| reward_template_id | UUID | NO | PK | Auto-generated |
| parent_id | UUID | NO | FK → User(id) | Constraint: User.role == PARENT |
| child_id | UUID | YES | FK → User(id) | NULL = family-level; NOT NULL = per-child |
| reward_type | ENUM | NO | {MONEY, TIME, CUSTOM_TEXT} | Display format determined by type |
| amount | DECIMAL(10,2) | NO | ≥ 0 | Dollars (MONEY), minutes (TIME), count (CUSTOM_TEXT) |
| unit | ENUM | NO | {PER_CHAPTER, PER_BOOK} | Granularity of reward |
| frequency | ENUM | NO | {IMMEDIATE, ON_COMPLETION} | When child sees accumulated reward |
| canned_template_id | VARCHAR | YES | Unique constraint if set | References canned option (e.g., "MONEY_050_PER_CHAPTER") |
| description | VARCHAR(255) | NO | | Display text: "Earn $0.50 per chapter" |
| is_deleted | BOOLEAN | NO | Default: FALSE | Soft-delete flag; archived rewards hidden from new selections |
| created_at | TIMESTAMP | NO | Auto | Audit |
| updated_at | TIMESTAMP | NO | Auto | Audit |

**Constraints**:
- If `child_id` IS NULL, `parent_id` uniquely identifies family-level template (scoped by parent)
- If `child_id` IS NOT NULL, `(parent_id, child_id, reward_type, unit)` is unique per child override
- `amount` must be positive or zero (no negative payouts)
- `is_deleted=true` hides from RewardSelector UI but allows existing selections to continue

---

### 2. RewardSelection

**Purpose**: Child's active choice of which reward to work toward for a specific book.

**Fields**:

| Field | Type | Nullable | Constraints | Notes |
|-------|------|----------|-------------|-------|
| selection_id | UUID | NO | PK | Auto-generated |
| child_id | UUID | NO | FK → User(id) | Constraint: User.role == CHILD |
| book_read_id | UUID | NO | FK → BookRead(id) | One reward per book |
| reward_template_id | UUID | NO | FK → RewardTemplate(id) | The reward selected |
| locked_amount | DECIMAL(10,2) | NO | | Snapshot of RewardTemplate.amount at selection time |
| locked_unit | ENUM | NO | {PER_CHAPTER, PER_BOOK} | Snapshot of RewardTemplate.unit at selection time |
| selected_at | TIMESTAMP | NO | Auto | When child made the choice |
| updated_at | TIMESTAMP | NO | | When reward was changed (if allowed) |
| is_active | BOOLEAN | NO | Default: TRUE | FALSE = reward changed, new selection active |

**Constraints**:
- `(book_read_id)` is unique: one reward per book (child selects reward when adding/starting book)
- Audit trail: `updated_at` tracks when child changes reward mid-book

---

### 3. ProgressTracking

**Purpose**: Track reading progress (chapters read or pages read) to calculate rewards.

**Fields**:

| Field | Type | Nullable | Constraints | Notes |
|-------|------|----------|-------------|-------|
| tracking_id | UUID | NO | PK | Auto-generated |
| book_read_id | UUID | NO | FK → BookRead(id) | Unique; one tracking per book |
| tracking_type | ENUM | NO | {CHAPTERS, PAGES, NONE} | Mode: chapter checkboxes, page slider, or skipped |
| total_chapters | INT | YES | ≥ 1 | OpenLibrary API provides, user can override |
| current_chapter | INT | YES | ≥ 0, ≤ total_chapters | Last chapter read (for resume) |
| chapters_read_list | TEXT (JSON) | YES | JSON array of chapter indices | Allows non-linear progress (e.g., [1,2,3,5,6]) |
| total_pages | INT | YES | ≥ 1 | OpenLibrary API provides, user can override |
| current_page | INT | YES | ≥ 0, ≤ total_pages | Last page read (for resume) |
| created_at | TIMESTAMP | NO | Auto | Audit |
| updated_at | TIMESTAMP | NO | | Last update when progress changed |

**Constraints**:
- If reward is PER_CHAPTER: `tracking_type` must be CHAPTERS and `total_chapters` must be populated before book completion
- If reward is PER_BOOK: `tracking_type` can be CHAPTERS, PAGES, or NONE
- `chapters_read_list` allows flexibility for kids who don't read sequentially

---

### 4. RewardAccumulation

**Purpose**: Immutable ledger of earned rewards (append-only transaction log).

**Fields**:

| Field | Type | Nullable | Constraints | Notes |
|-------|------|----------|-------------|-------|
| accumulation_id | UUID | NO | PK | Auto-generated |
| child_id | UUID | NO | FK → User(id) | Who earned it |
| book_read_id | UUID | NO | FK → BookRead(id) | Which book triggered it |
| reward_template_id | UUID | NO | FK → RewardTemplate(id) | Which reward was used |
| reward_type | ENUM | NO | {MONEY, TIME, CUSTOM_TEXT} | Denormalized for query speed |
| amount_earned | DECIMAL(10,2) | NO | ≥ 0 | Locked-in amount at time of earning |
| unit_count | INT | NO | ≥ 0 | Number of chapters/books that triggered reward |
| calculation_note | VARCHAR(255) | YES | | E.g., "5 chapters @ $0.50" or "Book complete @ $5.00" |
| status | ENUM | NO | {EARNED, PENDING_PAYOUT, PAID} | Lifecycle state |
| payout_date | TIMESTAMP | YES | Nullable | When parent marked PAID |
| created_at | TIMESTAMP | NO | Auto | When book was completed and reward earned |
| related_message_id | UUID | YES | FK → Message(id) | Link to payout reminder if child messaged |

**Constraints**:
- Insert-only table (no UPDATE after creation); status changes are new records if needed
- `amount_earned` is immutable audit trail (supports SC-003: zero discrepancies in spot checks)
- `unit_count` enables audit: spot-check "5 chapters" + "$0.50" = "$2.50"

---

### 5. Message

**Purpose**: In-app communication between parent and child (payout reminders, encouragement).

**Fields**:

| Field | Type | Nullable | Constraints | Notes |
|-------|------|----------|-------------|-------|
| message_id | UUID | NO | PK | Auto-generated |
| sender_id | UUID | NO | FK → User(id) | Parent or child |
| recipient_id | UUID | NO | FK → User(id) | Parent or child |
| message_type | ENUM | NO | {PAYOUT_REMINDER, ENCOURAGEMENT, PAYOUT_CONFIRMATION} | Flow type |
| message_text | VARCHAR(500) | NO | | E.g., "I've earned $12.50 and would like my payout!" |
| related_accumulation_id | UUID | YES | FK → RewardAccumulation(id) | Link to earnings if payout reminder |
| is_read | BOOLEAN | NO | Default: FALSE | Sender sees if recipient read |
| created_at | TIMESTAMP | NO | Auto | Audit |
| read_at | TIMESTAMP | YES | | When recipient viewed |

**Constraints**:
- Parent is always one of sender/recipient (preserves privacy of child-to-child messaging if needed later)
- `message_type` determines email eligibility: PAYOUT_REMINDER may trigger email (if parent opted in); others don't

---

### 6. Extension to BookRead (Existing Entity)

**Additional Fields** (new columns to existing table):

| Field | Type | Nullable | Constraints | Notes |
|-------|------|----------|-------------|-------|
| reward_selection_id | UUID | YES | FK → RewardSelection(id) | NULL until child selects reward |
| progress_tracking_id | UUID | YES | FK → ProgressTracking(id) | NULL until progress created |

---

## Database Constraints & Indices

### Primary Keys
- All new entities use UUID (`GenerationType.UUID`)

### Foreign Keys
- `RewardTemplate.parent_id` → `User.id` (referential integrity)
- `RewardTemplate.child_id` → `User.id` (optional; NULL = family-level)
- `RewardSelection.child_id` → `User.id`
- `RewardSelection.book_read_id` → `BookRead.id` (unique)
- `RewardSelection.reward_template_id` → `RewardTemplate.id`
- `ProgressTracking.book_read_id` → `BookRead.id` (unique)
- `RewardAccumulation.child_id` → `User.id`
- `RewardAccumulation.book_read_id` → `BookRead.id`
- `RewardAccumulation.reward_template_id` → `RewardTemplate.id`
- `Message.sender_id` → `User.id`
- `Message.recipient_id` → `User.id`

### Indices (for performance)
```sql
-- RewardTemplate
CREATE INDEX idx_reward_template_parent_id ON reward_templates(parent_id);
CREATE INDEX idx_reward_template_child_id ON reward_templates(child_id);
CREATE INDEX idx_reward_template_is_deleted ON reward_templates(is_deleted);

-- RewardSelection
CREATE INDEX idx_reward_selection_child_id ON reward_selections(child_id);
CREATE INDEX idx_reward_selection_book_read_id ON reward_selections(book_read_id);
CREATE INDEX idx_reward_selection_is_active ON reward_selections(is_active);

-- ProgressTracking
CREATE INDEX idx_progress_tracking_book_read_id ON progress_tracking(book_read_id);

-- RewardAccumulation
CREATE INDEX idx_accumulation_child_id ON reward_accumulations(child_id);
CREATE INDEX idx_accumulation_status ON reward_accumulations(status);
CREATE INDEX idx_accumulation_created_at ON reward_accumulations(created_at);

-- Message
CREATE INDEX idx_message_recipient_id ON messages(recipient_id);
CREATE INDEX idx_message_is_read ON messages(is_read);
CREATE INDEX idx_message_created_at ON messages(created_at);
```

---

## Validation Rules

### RewardTemplate
- `amount` ≥ 0 (allow $0 for "freemium" rewards)
- `unit` must be PER_CHAPTER or PER_BOOK
- `reward_type` must be one of {MONEY, TIME, CUSTOM_TEXT}
- If `child_id` is set, `parent_id` must reference a PARENT user
- If `child_id` is set, child must have `parent_id` matching the template's `parent_id` (family relationship validation)

### RewardSelection
- `child_id` user must have role == CHILD
- `book_read_id` must reference a book currently in progress or not started
- `reward_template_id` must not be deleted (`is_deleted` == false) or be a per-child override
- `locked_amount` and `locked_unit` must match `RewardTemplate` at time of selection

### ProgressTracking
- If `tracking_type` == CHAPTERS: `total_chapters` must be > 0
- If `tracking_type` == PAGES: `total_pages` must be > 0
- If `tracking_type` == NONE: `total_chapters`, `current_chapter`, `total_pages`, `current_page` all NULL
- `current_chapter` ≤ `total_chapters`; `current_page` ≤ `total_pages`

### RewardAccumulation
- `amount_earned` ≥ 0
- `unit_count` ≥ 0
- If amount_earned == 0, accumulation is logged but not displayed to child
- `status` lifecycle: EARNED → PENDING_PAYOUT (optional) → PAID (optional)

### Message
- `sender_id` ≠ `recipient_id` (no self-messages)
- `message_text` length ≤ 500 characters
- `message_type` == PAYOUT_REMINDER requires `related_accumulation_id`

---

## Default Values & Constants

### Canned Reward Templates

Inserted at application startup or migration (no parent_id, available to all):

```java
public enum CannedRewardTemplate {
    MONEY_050_PER_CHAPTER("$0.50 per chapter", MONEY, 0.50M, PER_CHAPTER),
    MONEY_500_PER_BOOK("$5.00 per book", MONEY, 5.00M, PER_BOOK),
    TIME_5MIN_PER_CHAPTER("5 min screentime per chapter", TIME, 5, PER_CHAPTER),
    TIME_120MIN_PER_BOOK("2 hours screentime per book", TIME, 120, PER_BOOK)
}
```

### Default Reward for New Children

When a new child is created with no explicit reward configuration:
- **Default**: MONEY_100_PER_CHAPTER ($1.00 per chapter)
- **Requirement**: FR-015, supports SC-007 (zero-config start)
- **Implementation**: Set during child account creation if no parent-configured reward exists

---

## Migration Strategy (Flyway)

**Phase 1 - New Tables** (V1__create_reward_customization.sql):
```sql
CREATE TABLE reward_templates (...);
CREATE TABLE reward_selections (...);
CREATE TABLE progress_tracking (...);
CREATE TABLE reward_accumulations (...);
CREATE TABLE messages (...);
```

**Phase 2 - Alter BookRead** (V2__extend_bookread_for_rewards.sql):
```sql
ALTER TABLE book_reads ADD COLUMN reward_selection_id UUID REFERENCES reward_selections(id);
ALTER TABLE book_reads ADD COLUMN progress_tracking_id UUID REFERENCES progress_tracking(id);
```

**Phase 3 - Indices** (V3__add_indexes_for_rewards.sql):
```sql
CREATE INDEX idx_reward_template_parent_id ON reward_templates(parent_id);
-- ... other indices
```

---

## Computed Views (Optional)

### Child Reward Summary View

```sql
SELECT 
  c.id as child_id,
  c.first_name,
  COALESCE(SUM(ra.amount_earned), 0) as total_earned,
  COALESCE(SUM(CASE WHEN ra.status='PAID' THEN ra.amount_earned ELSE 0 END), 0) as total_paid,
  COALESCE(SUM(CASE WHEN ra.status IN ('EARNED','PENDING_PAYOUT') THEN ra.amount_earned ELSE 0 END), 0) as available_balance
FROM users c
LEFT JOIN reward_accumulations ra ON c.id = ra.child_id
WHERE c.role = 'CHILD'
GROUP BY c.id, c.first_name;
```

This supports performance requirement **SC-005** (Rewards page load <1s).

---

## Next Steps

- **Phase 1 Contracts**: API contract definitions (request/response schemas)
- **Phase 1 Quickstart**: Example workflows and user journeys
- **Phase 2**: Task breakdown (implementation tasks per service/controller)
