# Data Model: Reading Rewards Parity Rebuild

## User

- **Purpose**: Represents a person who can authenticate into the system.
- **Core attributes**:
  - `id`
  - `role` as `PARENT` or `CHILD`
  - `email` for parents
  - `username` for children
  - `password`
  - `firstName`
  - `status` (`UNVERIFIED` or `VERIFIED`)
  - `verificationToken`
  - `parentId` for children
- **Rules**:
  - Parent accounts authenticate by email.
  - Child accounts authenticate by username.
  - Parent accounts require verification before access.
  - Child accounts are linked to exactly one parent.

## Book

- **Purpose**: Represents a book selected for tracking.
- **Core attributes**:
  - `googleBookId`
  - `title`
  - `authors`
  - `description`
  - `thumbnailUrl`
- **Rules**:
  - The external book identity is preserved through `googleBookId`.
  - Author ordering must remain stable for display parity.

## BookRead

- **Purpose**: Represents one reading lifecycle of a book by one user.
- **Core attributes**:
  - `id`
  - `userId`
  - `googleBookId`
  - `startDate`
  - `endDate`
- **Derived attributes**:
  - `inProgress` is derived from `endDate == null` and is not persisted as a separate column.
- **Rules**:
  - A user may have multiple book-read records for the same book over time.
  - Finishing a book marks the current lifecycle complete without deleting history.
  - Reread creates a new lifecycle record.

## Chapter

- **Purpose**: Represents a chapter definition for a tracked book.
- **Core attributes**:
  - `id`
  - `googleBookId`
  - `chapterIndex`
  - `name`
- **Rules**:
  - Chapter ordering is determined by `chapterIndex`.
  - Chapter names are editable after initial creation.

## ChapterRead

- **Purpose**: Represents the event that a user completed a chapter within a specific book-read lifecycle.
- **Core attributes**:
  - `id`
  - `bookReadId`
  - `chapterId`
  - `userId`
  - `completionDate`
- **Rules**:
  - Current implementation allows multiple reads for the same chapter/book-read pair unless clients prevent duplicates.
  - Removing a chapter-read entry must reverse its dependent reward side effect.

## Reward

- **Purpose**: Represents a financial or point-like event associated with reading or manual account actions.
- **Core attributes**:
  - `id`
  - `userId`
  - `type` as `EARN`, `SPEND`, or `PAYOUT`
  - `amount`
  - `note`
  - `chapterReadId` when reward is tied to progress
  - `createdAt`
- **Rules**:
  - Marking a chapter read creates an `EARN` reward.
  - Spending and payout entries alter current balance but do not erase earning history.
  - Summary totals are derived from all reward entries for the user.