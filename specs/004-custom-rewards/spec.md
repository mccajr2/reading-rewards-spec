# Feature Specification: Parent Reward Customization

**Feature Branch**: `[004-parent-reward-customization]`  
**Created**: 2026-05-12  
**Status**: Draft  
**Input**: User description: "I want to allow parents to customize rewards. Right now, we're hardcoding a dollar a chapter. I would like to have a couple defaults / canned options / and then allow for fully customizable rewards I'm not thinking of. Parents should be able to set these options globally (per family) or per kid. Parents should be able to create multiple reward options per kid. Kids should be able to select their reward type from the rewards list offered by their parents. Default to $1 per chapter as an initial option for parents (just so that there is a default), but make it overridable. If the payout is per book rather than per chapter, chapter tracking is optional. If the user wants to track progress, they should be able to give number of chapters (and cross them off as they go), or total pages (and update current page number as they go). Try to get page count from a book metadata source if possible (allow override though since this might be imperfect based on book edition, etc.). Rewards page should handle all reward types in an intuitive way. Accumulation and spending should be tracked. Kids should be able to nudge parents if they've earned rewards but they haven't been paid out - in app messages - linked to email. Parents should be able to send encouragement via in app messages - no email to kids. Also, parent accounts don't need rewards. No need to show that option."

## Clarifications

### Session 2026-05-12

- Q: For page-based rewards (example: 1 hour per 100 pages), how should earnings be calculated? → A: Milestone blocks only; earn at each full threshold and carry leftover pages forward.
- Q: When a child has multiple reward types (money and time-based), how should balances be tracked? → A: Separate balance and ledger per reward unit/type.
- Q: Who is allowed to mark rewards as paid out or spent? → A: Child requests; parent approves to finalize.
- Q: What should the nudge rate limit be per child? → A: One nudge every 24 hours.
- Q: When both family-level and child-specific reward options exist, what should the child see by default? → A: Both family and child-specific options (additive list).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Configure Reward Rules (Priority: P1)

As a parent, I can define reward rules at the family level and optionally override them per child so reward earning matches each child and household preferences.

**Why this priority**: Reward customization is the core feature request and unlocks all downstream reward behavior.

**Independent Test**: Can be fully tested by creating family-level defaults, adding per-child overrides, and verifying saved reward options appear correctly for each child.

**Acceptance Scenarios**:

1. **Given** a parent has no custom rewards yet, **When** they open reward settings, **Then** they see an initial default reward of $1 per chapter and can edit or remove it.
2. **Given** a parent creates multiple reward options for one child, **When** they save changes, **Then** those options are available only to that child unless explicitly marked as family-wide.
3. **Given** a parent defines family-wide reward options, **When** a child without overrides views available rewards, **Then** the child sees the family-wide options.

---

### User Story 2 - Select and Earn Rewards (Priority: P1)

As a child, I can choose one of the reward options my parent has offered and earn toward that reward as I log reading progress.

**Why this priority**: Children need a clear and motivating earning path tied to parent-approved rewards.

**Independent Test**: Can be fully tested by selecting a reward option, recording reading activity, and verifying earnings accumulate using the selected rule.

**Acceptance Scenarios**:

1. **Given** a child has multiple offered rewards, **When** they select one as active, **Then** new reading activity accrues earnings against the selected reward.
2. **Given** a child changes their selected reward, **When** future reading is logged, **Then** only future earnings follow the newly selected rule and historical earnings remain unchanged.
3. **Given** a parent account user, **When** they access rewards views, **Then** they are not presented with child reward selection workflows.

---

### User Story 3 - Track Reading Progress by Book, Chapter, or Pages (Priority: P2)

As a parent or child, I can log progress using the tracking method that best fits a book and reward rule so progress is practical for different book types.

**Why this priority**: Flexible tracking is required for per-chapter, per-book, and per-page based rewards.

**Independent Test**: Can be fully tested by creating one per-book reward and one chapter/page-based reward, then logging progress through each tracking method.

**Acceptance Scenarios**:

1. **Given** a reward is defined as per book, **When** a user marks the book complete without chapter entries, **Then** earnings are granted without requiring chapter tracking.
2. **Given** a book has chapter tracking enabled, **When** a child checks off completed chapters, **Then** progress and earnings update according to the selected reward rule.
3. **Given** a book has page tracking enabled, **When** current page is updated, **Then** progress and earnings update from page-based milestones.
4. **Given** book metadata provides a suggested page count, **When** a parent reviews the value, **Then** they can accept it or override it manually.

---

### User Story 4 - Manage Payouts and Messages (Priority: P2)

As a child and parent, we can manage unpaid rewards through in-app communication so earned rewards are visible, nudges are possible, and payout status is clear.

**Why this priority**: Transparency and communication are necessary for reward follow-through and motivation.

**Independent Test**: Can be fully tested by earning unpaid rewards, sending child nudges, sending parent encouragement, and recording payout/spend actions.

**Acceptance Scenarios**:

1. **Given** a child has earned rewards that are still unpaid, **When** the child sends a nudge, **Then** the parent receives an in-app message and a parent-directed email notification.
2. **Given** a parent wants to motivate a child, **When** they send encouragement, **Then** the child receives an in-app message and no email is sent to the child.
3. **Given** a child requests payout or spending, **When** a parent approves the request, **Then** the ledger is finalized with paid/spent entries and updated balances.
4. **Given** rewards are paid out or spent, **When** either party views reward history, **Then** accumulation, payout, and spending entries are shown with current balances.

---

### Edge Cases

- A child has no assigned or family reward options available yet.
- A parent removes a reward option currently selected by a child.
- A reward rule change occurs after earnings already accumulated under an older rule.
- A per-page rule is selected but total page count is missing or later corrected.
- A child sends repeated nudges in a short period.
- Book metadata returns no page count or an inaccurate page count for the selected edition.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide at least one initial default reward option of $1 per chapter for each family.
- **FR-002**: Parents MUST be able to edit, disable, and replace the initial default reward option.
- **FR-003**: Parents MUST be able to create multiple reward options at family scope.
- **FR-004**: Parents MUST be able to create multiple reward options at per-child scope.
- **FR-005**: System MUST resolve a child’s available reward list as an additive list that combines active family-scope options and active child-specific options.
- **FR-006**: Children MUST be able to select one offered reward option as their active earning rule.
- **FR-007**: System MUST calculate earnings based on the active reward rule and logged reading progress.
- **FR-008**: System MUST support reward rule units including per chapter, per page milestone, and per completed book.
- **FR-009**: For per-book rewards, system MUST allow book completion without requiring chapter tracking.
- **FR-010**: System MUST support optional chapter tracking with chapter count entry and chapter completion updates.
- **FR-011**: System MUST support optional page tracking with total page count and current page updates.
- **FR-011a**: For page-based milestone rewards, system MUST grant earnings only when each full page threshold is reached and MUST carry any leftover pages toward the next threshold.
- **FR-012**: System MUST attempt to prefill total page count from an external book metadata source when available and MUST allow parent override.
- **FR-013**: System MUST track reward transactions as separate types: earned, paid out, and spent.
- **FR-013a**: System MUST maintain separate balances and ledger streams per reward unit/type and MUST NOT combine unlike units into a single balance.
- **FR-013b**: Children MUST be able to request payout or spending actions, and only parent approval can finalize paid-out or spent ledger entries.
- **FR-014**: Children MUST be able to send nudge messages about unpaid earned rewards to parents via in-app messaging.
- **FR-014a**: System MUST enforce a nudge cooldown of one nudge per child per 24-hour window.
- **FR-015**: System MUST send parent-directed email notifications for child nudge messages.
- **FR-016**: Parents MUST be able to send encouragement messages to children via in-app messaging.
- **FR-017**: System MUST NOT send email notifications to children for encouragement messages.
- **FR-018**: Parent account interfaces MUST NOT display child reward earning/selection workflows intended for child users.
- **FR-019**: Rewards pages MUST present all supported reward types in one consistent flow that allows users to understand available options, active selection, current progress, and current balance.

### Key Entities *(include if feature involves data)*

- **Reward Option**: A parent-defined rule with scope (family or child), unit type (chapter, page milestone, completed book), payout value, status, and optional title/description.
- **Child Reward Selection**: A child’s active chosen reward option and effective period.
- **Reading Progress Record**: Progress entries per book using chapter completion, current page updates, or completion status.
- **Reward Ledger Entry**: A dated transaction representing earned, paid out, or spent reward value with running balance impact.
- **Message Thread**: In-app conversation items between parent and child, including message type (nudge or encouragement) and delivery channel outcomes.
- **Book Metadata Snapshot**: Suggested book details (for example total pages) captured at selection time with manual override value.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 95% of parents can create or modify a family reward option in 2 minutes or less.
- **SC-002**: 95% of children can select an offered reward option in 30 seconds or less.
- **SC-003**: For a controlled test set of reading logs, reward earning totals match expected calculations in at least 99% of cases.
- **SC-004**: 90% of users in usability testing can correctly identify current reward balance, unpaid earned amount, and selected reward type without assistance.
- **SC-005**: At least 90% of child nudge messages about unpaid rewards are viewed by parents within 24 hours.
- **SC-006**: Support requests related to reward rule confusion decrease by at least 40% within one release cycle after launch.

## Assumptions

- Parent users remain the only users who can create or modify reward rules.
- Each child has at most one active reward selection at a time, but can switch among available options.
- If both chapter and page tracking are enabled for a book, the family uses one primary earning basis defined by the selected reward rule.
- Parent-directed email notification for child nudges uses existing parent contact and notification preferences.
- External book metadata can be unavailable or imperfect; manual override is always authoritative.
- Existing authentication, family relationships, and reading log workflows remain in scope and are reused.
