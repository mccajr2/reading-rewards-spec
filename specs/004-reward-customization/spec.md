# Feature Specification: Customizable Family Rewards

**Feature Branch**: `004-reward-customization`  
**Created**: 2026-05-11  
**Status**: Draft  
**Input**: Parent-customizable reward system with canned options, family-level and per-child configuration, kid-friendly reward selection, and in-app messaging

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Parent Configures Global Reward Defaults (Priority: P1)

Parents want to define baseline reward options that apply to all children in the family without needing to set up rewards per child. This enables quick family-wide consistency while still allowing per-child overrides.

**Why this priority**: Foundational feature—parents need a simple way to establish reward structure across the family. Directly addresses the shift from hardcoded $1/chapter.

**Independent Test**: Parent logs in, navigates to Rewards Settings, sees default reward options (canned + custom), sets global family defaults (e.g., "$0.50 per chapter", "5 min screentime per chapter"), and all children immediately inherit these options.

**Acceptance Scenarios**:

1. **Given** a parent account with no reward configuration, **When** parent accesses Rewards Settings, **Then** system displays canned reward templates and a custom reward builder
2. **Given** a parent has selected a canned reward template (e.g., "$0.50 per chapter"), **When** parent saves it as global, **Then** all children under this parent can select from this reward option
3. **Given** global rewards are configured, **When** a new child account is created, **Then** child inherits access to existing global reward options
4. **Given** global rewards exist, **When** parent overrides a specific reward for one child, **Then** global setting remains unchanged and only that child's reward differs
5. **Given** parent deletes a global reward, **When** parent confirms deletion, **Then** system archives the reward (soft-delete) instead of permanent deletion, allowing recovery if needed; all per-child variants remain active and linked to the archived template

---

### User Story 2 - Parent Creates Per-Child Custom Rewards (Priority: P1)

Parents need to set different reward incentives for different children, recognizing that individual kids have different motivations (one kid likes screentime, another prefers money). Per-child rewards override global defaults without affecting siblings.

**Why this priority**: Enables personalization which increases engagement—critical for diverse family dynamics.

**Independent Test**: Parent creates one reward for Child A ($1/chapter), a different reward for Child B (30 min screentime/chapter), and a third per-chapter+per-book hybrid for Child C. Each child independently sees only their assigned rewards.

**Acceptance Scenarios**:

1. **Given** a parent with multiple children, **When** parent creates a new reward for Child A, **Then** system allows selecting reward type (money per chapter/book, time per chapter/book, custom text) and sets it as available only to that child
2. **Given** Child A has 2 rewards and Child B has 1, **When** Child A logs in, **Then** Child A sees only their 2 reward options; Child B sees only their 1 option
3. **Given** global rewards exist and per-child reward is created for one child, **When** that child logs in, **Then** system shows both global and per-child reward options
4. **Given** a parent has created multiple per-child rewards, **When** parent views the reward management page, **Then** system displays rewards grouped by child with clear visual hierarchy (global vs. per-child)

---

### User Story 3 - Parent Defines Canned Reward Options (Priority: P1)

System provides built-in reward templates (e.g., "$0.50 per chapter", "5 min screentime/chapter", "$5 per book", "2 hrs screentime/book") as quick-start options. Parents can select one or more and customize if needed.

**Why this priority**: Reduces friction—parents don't need to think from scratch; they can pick an option and go.

**Independent Test**: Parent opens Rewards Settings and sees pre-configured options like "$0.50/chapter", "$5/book", "5 min screentime/chapter", "2 hrs screentime/book" and can select any or create fully custom.

**Acceptance Scenarios**:

1. **Given** no prior reward configuration, **When** parent views canned templates, **Then** system displays at least 4 templates: (a) $0.50/chapter, (b) $5/book, (c) 5 min screentime/chapter, (d) 2 hrs screentime/book
2. **Given** parent has selected a canned template, **When** parent saves it, **Then** system stores the reward with all default parameters (amount, unit, granularity)
3. **Given** canned reward is saved, **When** child logs in, **Then** child can select that reward option as incentive for their reading

---

### User Story 4 - Child Selects Personal Reward Target (Priority: P1)

Instead of parents assigning rewards, children choose which of the available parent-defined rewards they want to work toward. This increases buy-in and engagement.

**Why this priority**: Kids are more motivated when they have choice. Direct user feedback from reading-list/rewards flows shows kid engagement drives retention.

**Independent Test**: Child logs in, views available rewards (global + per-child set by parent), selects a reward type (e.g., "$0.50 per chapter" or "30 min screentime per book"), and system begins tracking progress against that reward.

**Acceptance Scenarios**:

1. **Given** parent has configured global and per-child rewards, **When** child logs in, **Then** child sees all available rewards with clear descriptions (e.g., "You can earn $0.50 for each chapter you read")
2. **Given** child has not yet selected a reward, **When** child is adding a book, **Then** system prompts "Pick a reward goal for this book" or suggests default ($1/chapter)
3. **Given** child selects a reward goal, **When** child completes a book, **Then** system calculates reward owed (e.g., "You've earned $5.00 from 10 chapters at $0.50 each")
4. **Given** multiple reward types exist, **When** child selects one, **Then** system applies that reward type to all new reading until child changes selection

---

### User Story 5 - Parent and Child Track Reward Accumulation & Payouts (Priority: P1)

Both parent and child see accumulated rewards over time, what has been paid out, and what is pending. This builds trust and transparency.

**Why this priority**: Without visibility into earnings, kids lose motivation. Parents need accountability records for payout tracking.

**Independent Test**: Child completes books, rewards accumulate, both child and parent see running total ("You've earned $12.50 total, spent $0.00, available: $12.50"). Parent marks payout completed, balances update.

**Acceptance Scenarios**:

1. **Given** child has earned $5.00 from completed chapters, **When** child opens Rewards page, **Then** system displays "Total Earned: $5.00", "Available: $5.00", "Spent: $0.00"
2. **Given** child has earned $5.00 and parent has marked $3.00 as paid out, **When** child views balance, **Then** system shows "Total Earned: $5.00", "Available: $2.00", "Spent: $3.00"
3. **Given** pending rewards exist, **When** parent logs in, **Then** parent sees child's accumulated balance and can mark payouts as complete
4. **Given** payout is marked complete, **When** child views Rewards page, **Then** available balance updates to reflect payout

---

### User Story 6 - Progress Tracking: Chapters vs. Pages (Priority: P2)

If reward is "per chapter", chapter tracking is mandatory. If reward is "per book", chapter tracking is optional. Kids can track progress by chapters (incrementally) or by page count (total pages or current page), with OpenLibrary API providing default page counts.

**Why this priority**: Flexible progress tracking accommodates different reading styles (chapter readers vs. page readers). Per-book rewards with optional tracking reduces friction for quick reads.

**Independent Test**: Parent adds a book with "$5 per book" reward. Child can either (a) manually enter chapter count and check off chapters, (b) enter total page count and update current page, or (c) skip tracking and just mark book as complete. For per-chapter rewards, tracking is mandatory before book can be completed.

**Acceptance Scenarios**:

1. **Given** a book has reward type "$5 per book", **When** child adds book, **Then** system offers: track chapters, track pages, or skip tracking and mark complete
2. **Given** a book with "$0.50 per chapter" reward, **When** child tries to complete without entering chapters, **Then** system displays error "Chapter count required to complete this book"
3. **Given** book is added and OpenLibrary API returns page count, **When** child views book detail, **Then** system displays suggested page count and allows override
4. **Given** child selects "track by chapters", **When** child completes book, **Then** system shows all chapter checkboxes and calculates reward based on checked chapters
5. **Given** child selects "track by pages", **When** child updates "current page", **Then** system recalculates progress percentage and (if applicable) chapter equivalents
6. **Given** OpenLibrary API cannot find page count, **When** child adds book, **Then** system prompts "No page data found, enter manually" and provides text input

---

### User Story 7 - Kids Nudge Parents for Pending Payouts (Priority: P2)

Kids can send in-app messages to parents saying "I've earned $X but haven't been paid yet" to prompt parent action. Optionally links to email to ensure parents don't miss.

**Why this priority**: Kids' messaging increases parent accountability and reduces friction. In-app first respects kid privacy; optional email escalation gives parents control.

**Independent Test**: Child has earned $10 but status is unpaid. Child clicks "Send Payout Reminder" or similar, parent receives in-app notification + (optionally) email. Parent responds in-app to confirm payment.

**Acceptance Scenarios**:

1. **Given** child has accumulated unspent rewards, **When** child views Rewards page, **Then** system displays "Pending Payout: $X" with button "Notify Parent"
2. **Given** child clicks "Notify Parent", **When** message is sent, **Then** parent receives in-app notification with message "I've earned $X and would like my payout!"
3. **Given** parent receives in-app notification, **When** parent marks payout as complete, **Then** child receives in-app confirmation and balance updates
4. **Given** in-app payout reminder is sent, **When** parent has email notifications enabled, **Then** parent may receive email [NEEDS CLARIFICATION: email on every payout reminder, or only if parent opts in, or never?]
5. **Given** payout reminder was sent, **When** parent reviews pending payouts, **Then** system shows child's earnings, reminder timestamp, and action history

---

### User Story 8 - Parents Send Encouragement Messages (Priority: P2)

Parents can send in-app encouragement messages ("Great job on that book!", "Keep it up!") directly to children. No email is sent to children (in-app only) to protect privacy and avoid notification fatigue.

**Why this priority**: Engagement feature. Parent feedback loops increase motivation without overloading kids' inboxes.

**Independent Test**: Parent sends message "You're a reading superstar!" to Child A. Child A receives in-app notification and can view message in messaging center. No email is sent.

**Acceptance Scenarios**:

1. **Given** parent is viewing a child's profile, **When** parent clicks "Send Message" or similar, **Then** system opens a text input for encouragement
2. **Given** parent types and sends encouragement message, **When** message is delivered, **Then** child receives in-app notification only (no email)
3. **Given** child has received messages from parent, **When** child opens Messages or Inbox, **Then** system displays all parent encouragement messages with timestamps
4. **Given** encouragement message is displayed to child, **When** child views it, **Then** message does not expire or auto-delete (persisted in history)

---

### User Story 9 - No Rewards Shown for Parent Accounts (Priority: P1)

Parent accounts do not have a Rewards page. Reward system is child-only. Parents see rewards management (configure & track), not personal rewards.

**Why this priority**: Parents don't earn rewards; they manage them. UI clarity and reduced confusion.

**Independent Test**: Parent logs in, navigates to Rewards, sees Rewards Management page (configure family/child rewards, view pending payouts) but no "My Rewards" or personal balance. Child logs in, sees personal Rewards page (earnings, available balance, reward options, messaging).

**Acceptance Scenarios**:

1. **Given** a parent account, **When** parent navigates to Rewards section, **Then** system displays rewards configuration & management interface, not personal rewards interface
2. **Given** main navigation is viewed by parent, **When** parent looks for Rewards, **Then** navigation link says "Manage Rewards" or similar (not "My Rewards")
3. **Given** child account is logged in, **When** child navigates to Rewards, **Then** system displays personal rewards page (available rewards, earnings balance, payout history)

---

### User Story 10 - Rewards Page Handles All Reward Types (Priority: P2)

Rewards page intuitively displays any reward type (money, screentime, custom) with appropriate accumulation and spending visualizations. Money rewards show currency, screentime shows hours/minutes, custom types display as-is.

**Why this priority**: UX consistency. System should gracefully handle any reward type parent creates without special-casing.

**Independent Test**: Child has 3 different reward types active ($0.50/chapter, 30 min screentime/book, custom "Movie Night"). Rewards page displays all three with appropriate formatting and accumulated totals.

**Acceptance Scenarios**:

1. **Given** child is working toward "$0.50 per chapter" reward, **When** child views Rewards page, **Then** system displays accumulated amount in currency format (e.g., "$5.50")
2. **Given** child is working toward "30 min screentime per chapter" reward, **When** child views Rewards page, **Then** system displays accumulated time (e.g., "150 minutes" or "2 hours 30 minutes")
3. **Given** custom reward type is configured (e.g., "Movie Night"), **When** child works toward it, **Then** system displays it as a count ("You've earned 3 Movie Nights") and accumulates until parent marks payouts as complete
4. **Given** multiple reward types coexist, **When** child views consolidated Rewards page, **Then** each reward type is visually distinct and clearly labeled

---

### Edge Cases

- What happens if parent deletes a reward that a child is actively working toward? [NEEDS CLARIFICATION: pause tracking, convert to custom, notify child?]
- How does system handle reward changes mid-book (e.g., parent changes payout rate while child is reading)? [NEEDS CLARIFICATION: apply to new books only, or backfill current book?]
- Can parent disable/archive a reward without deleting it (for historical tracking)? [Assumed yes, but not explicitly required]
- What if child hasn't earned any rewards yet—is Rewards page still shown? [Assumed yes, empty state with instructions]
- Can child change reward selection mid-book? [Assumed only for per-book rewards, not mid-chapter]

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow parents to view, create, edit, and delete reward configurations at family level
- **FR-002**: System MUST allow parents to create per-child reward overrides that supersede family-level rewards
- **FR-003**: System MUST provide at least 4 canned reward templates: $0.50/chapter, $5/book, 5 min screentime/chapter, 2 hrs screentime/book
- **FR-004**: System MUST allow parents to define fully custom rewards with configurable unit, amount, and granularity
- **FR-005**: System MUST display all available rewards (global + per-child) to child users in an intuitive selection interface
- **FR-006**: System MUST track accumulated rewards for each child by reward type and persist history
- **FR-007**: System MUST calculate and display pending vs. paid-out rewards separately
- **FR-008**: System MUST support progress tracking by chapters (mandatory for per-chapter rewards, optional for per-book rewards)
- **FR-009**: System MUST support progress tracking by page count (optional, total pages or current page)
- **FR-010**: System SHOULD attempt to fetch page count from OpenLibrary API for a book, with manual override option
- **FR-011**: System MUST allow children to send in-app payout reminder messages to parents
- **FR-012**: System MUST notify parents of pending payout reminders via in-app message; system SHOULD send email notifications for payout reminders with email notifications enabled by default in parent settings (parent can opt out)
- **FR-013**: System MUST allow parents to send encouragement messages to children via in-app only (no email)
- **FR-014**: System MUST NOT display Rewards or personal balance interface for parent accounts
- **FR-015**: System MUST default new children to $1/chapter reward if no global or per-child reward is explicitly configured
- **FR-016**: System MUST handle multiple reward types (currency, time, custom text) with appropriate display formatting

### Key Entities

- **Reward**: Represents a reward template (family-level or per-child). Attributes: reward_id, parent_id, child_id (optional), reward_type (money/time/custom), amount, unit (chapter/book), frequency (per_chapter/per_book), canned_template_id (if applicable), created_at, updated_at, is_deleted
- **Child Reward Selection**: Represents which reward a child is currently working toward. Attributes: selection_id, child_id, reward_id, selected_at, is_active
- **Reward Accumulation**: Represents earned rewards. Attributes: accumulation_id, child_id, reward_id, book_read_id, amount_earned, unit_count, is_paid_out, payout_date, created_at
- **Progress Tracking**: Represents chapter or page progress for a book. Attributes: tracking_id, book_read_id, tracking_type (chapters/pages), total_chapters (optional), current_chapter (optional), total_pages (optional), current_page (optional)
- **Message**: Represents in-app communication. Attributes: message_id, sender_id (parent or child), recipient_id, message_text, message_type (payout_reminder/encouragement), related_reward_id (optional), created_at, is_read

### Data and Integration Points

- **OpenLibrary API**: Query `https://openlibrary.org/api/books?isbn=<isbn>` to fetch page_count; fallback to manual entry if API unavailable or returns null
- **Email Service**: Optional integration for parent payout reminder notifications [NEEDS CLARIFICATION on config]
- **Database**: Persist new Reward, Progress Tracking, and Message entities; extend BookRead to include reward_id reference

## Success Criteria *(mandatory)*

- **SC-001 (Adoption)**: 80% of parents configure at least one custom or canned reward within first week of feature launch
- **SC-002 (Engagement)**: Children's daily reading frequency increases by 15% post-launch (measured over 4-week period)
- **SC-003 (Accuracy)**: Reward calculations are 100% accurate; spot-checks of 50 random payouts show zero discrepancies
- **SC-004 (Visibility)**: 95% of payout reminders sent by children are acknowledged by parent within 48 hours
- **SC-005 (Performance)**: Rewards page loads in under 1 second on 4G throttled connection (measured via Lighthouse)
- **SC-006 (Accessibility)**: Rewards UI passes WCAG 2.1 AA accessibility audit with zero critical violations
- **SC-007 (Default Assumption)**: All new children default to $1/chapter if no override is configured, allowing zero-config start

## Assumptions

- Parents will use email notifications for payout reminders (config TBD)
- OpenLibrary API is the primary page-count source; users accept that page counts may vary by edition
- In-app messages are the primary communication channel; email is supplementary for parents only
- Children's messaging interface is simple and non-exploitable (no free-text comments, only predefined payout reminders and parent-sent messages)
- Reward types are limited to money, time, and short custom text; complex reward structures are out of scope
- Bulk reward operations (create 1 reward for all 5 kids) are out of scope; per-child configuration is manual
- Historical reward data retention is indefinite (no purge policy defined)

## Dependencies & Risks

- **OpenLibrary API Availability**: If API is down, feature degrades gracefully with manual page entry
- **Database Schema Extension**: BookRead, Child, Parent tables require new foreign key columns; schema migration needed
- **Parent Acceptance**: Risk that complex reward management discourages parents from using feature; mitigation: start with canned templates and simple UX
- **Kid Messaging Abuse**: Risk that kids send excessive payout reminders; mitigation: rate-limit reminders or require parent acknowledgment per reminder
- **Payout Audit Trail**: Parents must be able to provide proof of payout to kids; messaging history is critical audit log

---

**Status**: Ready for clarification questions (Q1–Q3 from edge cases and marked sections)
