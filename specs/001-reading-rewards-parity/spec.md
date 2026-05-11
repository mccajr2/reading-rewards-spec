# Feature Specification: Reading Rewards Parity Rebuild

**Feature Branch**: `001-reading-rewards-parity`  
**Created**: 2026-05-07  
**Status**: Active Baseline  
**Input**: User description: "Analyze the current Reading Rewards codebase, reverse engineer it into specs, rebuild it in a new repository, keep behavior the same, modernize the implementation and toolchains, add unit tests, and make the result easy to maintain."

## Current Implementation Snapshot

- Parent signup, email verification, login, and logout are implemented in backend and frontend.
- The verification page now calls backend token verification and displays backend success/error messages.
- Parent kid management, reading-progress, history, and rewards flows are implemented.
- Backend and frontend automated tests are in place and passing locally.
- End-to-end Playwright tests are implemented across auth, parent, and reading journeys. The suite passes when the local stack is running and healthy.

## Clarifications

### Session 2026-05-08

- Q: Should parent accounts be admin-only (no book adding/scanning) or hybrid (can add own books)? → A: Hybrid-lite. Parents can manage kids and see reporting, plus optionally add their own books (separate from kids). Kids navigation is good and should remain unchanged.
- Q: What should the parent dashboard display? → A: Per-child cards with rollup and drill-down. Dashboard shows each child's summary card (name, books in progress, completed count, current balance). Click to drill into that child's detail.
- Q: What can parents do in child detail view? → A: Read-only + reversal. Parent can view child's current books (in progress + finished), chapters, current balance, reward history. Parent can reverse a chapter-read entry to correct mistakes, and its associated reward is reversed automatically.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Parent account lifecycle and dashboard work end-to-end (Priority: P1)

A parent can sign up, verify their email address, log in, manage child accounts, and view a dashboard showing all children's reading progress and reward data without losing any behavior currently supported by the legacy app. Parent accounts are primarily administrative for child management/reporting and can optionally add books only to their own separate parent reading list.

**Why this priority**: Parent onboarding, child management, and reporting are the administrative entry points for the product. If this flow breaks, the rest of the system is unusable for families.

**Independent Test**: Can be fully tested by signing up a parent, verifying the account, logging in, creating a child, resetting that child's password, and viewing the parent dashboard with child progress summaries.

**Acceptance Scenarios**:

1. **Given** a new parent submits valid signup details, **When** the system creates the account, **Then** the parent account is stored unverified and a verification email workflow is triggered.
2. **Given** a parent account has a valid verification token, **When** the parent opens the verification link, **Then** the account becomes verified and the token can no longer be reused.
3. **Given** a verified parent is authenticated, **When** the parent creates a child account, **Then** the child can log in with a username and password and is linked only to that parent.
4. **Given** an authenticated parent owns a child account, **When** the parent resets the child's password, **Then** the child can authenticate with the new password and previous access is invalidated.
5. **Given** an authenticated parent has one or more children with reading data, **When** the parent opens the parent dashboard, **Then** the dashboard displays a per-child summary card showing each child's name, books in progress, completed books count, and current reward balance.
6. **Given** a parent is viewing the parent dashboard, **When** the parent clicks on a child's card, **Then** the parent drills into that child's detail view showing all books (in-progress and finished), chapters for each book, and complete reward history.

---

### User Story 2 - Child reading progress and rewards remain consistent (Priority: P1)

A child can search for books, add a book, manage chapters, mark chapters as read, finish books, reread books, and see reward totals that match the legacy app's behavior.

**Why this priority**: Reading progression and reward earning are the product's core business behavior.

**Independent Test**: Can be fully tested by logging in as a child, adding a searched book, adding chapters, reading chapters, checking rewards, finishing the book, and starting a reread.

**Acceptance Scenarios**:

1. **Given** an authenticated child searches by title, author, or ISBN, **When** matching books are found, **Then** the child can add one to the reading list with its chapter data.
2. **Given** an in-progress book with unread chapters, **When** the child marks a chapter as read, **Then** the read state is stored and an earn-type reward is recorded for that chapter.
3. **Given** a chapter read entry exists for a child, **When** the child removes that read marker, **Then** the associated chapter-read state and reward side effect are reversed consistently.
4. **Given** a child finishes a book and later chooses reread, **When** the reread action succeeds, **Then** a new in-progress reading record is created without deleting the prior completed history.
5. **Given** a child adds a book that has no saved chapter list, **When** the add flow opens an in-app dialog asking for chapter count, **Then** the system seeds default chapter names (`Chapter 1..N`) that are shared for future readers of the same book.
6. **Given** chapter definitions already exist for a book, **When** another child adds that same book, **Then** the existing shared chapter definitions are reused without re-seeding.

---

### User Story 3 - Parent child detail view and reversal capability (Priority: P2)

Parents can drill into any child's account to see all reading progress and reward transactions, and can reverse chapter-read entries to correct data entry mistakes. Children can inspect their own history and rewards without a behavior regression.

**Why this priority**: Parent oversight and data correction capability is central to trust and usability for ongoing use, especially when children make mistakes or parents need to audit activity.

**Independent Test**: Can be fully tested by seeding reading and reward data in a child account, viewing the parent drill-down, and reversing a chapter-read entry to verify it removes the associated reward.
**Acceptance Scenarios**:

1. **Given** a parent is viewing a child's detail page, **When** the parent sees the complete reading history (books in-progress, finished, chapters read), **Then** the parent can see books with their chapter counts, chapters marked as read, and the reward earned for each chapter.
2. **Given** a parent is viewing a child's detail page with reward history, **When** the parent identifies an erroneous chapter-read or reward entry, **Then** the parent can trigger a reversal action that removes the chapter-read entry and its associated reward without changing child's other data.
3. **Given** a child has prior completed reading activity, **When** the child opens history and rewards, **Then** the child sees completed reading records and paginated reward history with the same summary math as the legacy app.

### Edge Cases

- A parent login attempt before verification must be rejected with the same access rule as the legacy system.
- A child login must work via username even though parent login uses email.
- Duplicate chapter-read actions for the same book-read and chapter must not create duplicate rewards.
- Deleting a book-read must remove dependent chapter-read and reward records consistently.
- Missing or invalid verification tokens must fail without partially changing account state.
- Unauthorized API requests must be rejected and frontend auth state must recover cleanly.
- A parent may NOT directly add books or mark chapters read on behalf of a child; all child reading actions must originate from the child account.
- A parent may reverse (undo) a child's chapter-read entry, which must also reverse its corresponding reward entry; the reversal action is an explicit parent operation, not automatic.
- A child accessing their own dashboard must NOT see their parent's books or reading data; parent and child reading lists are separate.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST expose an authentication flow for parent signup, email verification, login, and logout that preserves legacy route semantics.
- **FR-002**: The system MUST allow authenticated parents to create and manage child accounts that authenticate by username and password.
- **FR-003**: The system MUST preserve the current route-level authorization boundaries between unauthenticated users, parents, and children.
- **FR-004**: The system MUST support book search by title, author, and ISBN and allow authenticated children to add books to their reading list. Parents can optionally search and add books to their own separate reading list.
- **FR-005**: The system MUST preserve chapter management, chapter rename, chapter-read creation, and chapter-read reversal behavior, including add-time chapter-count seeding for new books through the application UI (not a browser-native prompt) and shared chapter reuse for subsequent readers.
- **FR-006**: The system MUST create and summarize reward entries with the same earn, spend, and payout semantics used by the legacy app unless an approved spec explicitly changes them.
- **FR-007**: The system MUST support finishing and rereading books while preserving existing reading history.
- **FR-008**: The system MUST expose parent summary and child history views that match the legacy app's information model. Specifically, parent dashboard MUST display per-child summary cards (name, books-in-progress count, finished books count, current reward balance) and allow drill-down into each child's detail view.
- **FR-008a**: The system MUST allow parents to reverse (undo) a child's chapter-read entry from the child detail view, which automatically reverses the associated reward entry.
- **FR-008b**: Parent and child reading lists MUST be completely separate; a parent cannot add books on behalf of a child, and a child cannot see parent's reading data.
- **FR-009**: The system MUST provide deterministic local development and test setup using environment templates rather than committed live secrets.
- **FR-010**: The system MUST include backend, frontend, and end-to-end automated tests for the critical journeys covered by this spec. See SC-002.

### Key Entities *(include if feature involves data)*

- **User**: Represents a parent or child account with authentication credentials, verification status, and parent-child relationship rules.
- **Book**: Represents a catalog item selected for tracking, including identity, descriptive fields, and author information.
- **BookRead**: Represents one reading lifecycle for a book by a specific user, including in-progress or completed state.
- **Chapter**: Represents a chapter definition for a tracked book.
- **ChapterRead**: Represents a user's completed reading event for a specific chapter inside a specific book-read lifecycle.
- **Reward**: Represents an earn, spend, or payout financial event linked to a user and optionally to reading progress.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The successor app reproduces the approved parent and child core workflows without contract drift in endpoint path, status behavior, or required payload fields for the covered journeys.
- **SC-002**: Automated backend, frontend, and end-to-end tests cover P1 user stories and pass in local execution. All three test layers are implemented and passing.
- **SC-003**: The new repository can be bootstrapped from documented environment templates without requiring secrets committed to source control.
- **SC-004**: The successor repository isolates its own toolchains, configuration, and git history from the legacy repository while remaining traceable to the legacy behavior baseline.

## Assumptions

- The legacy repository at `/Users/jasonmccarthy/projects/reading-rewards` remains available as the behavior reference during the rebuild.
- Styling can change as long as the core information architecture and user flows remain understandable and complete.
- The successor implementation may fix security and maintainability issues as long as those fixes do not silently change approved product behavior.
- Latest practical stable toolchains will be selected based on ecosystem support rather than chasing unsupported edge releases.