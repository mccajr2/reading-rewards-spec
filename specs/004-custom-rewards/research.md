# Research: Parent Reward Customization

**Phase**: 0 | **Feature**: `004-custom-rewards` | **Date**: 2026-05-12

## Decision Log

### 1. Reward Rule Model and Scope Resolution

**Decision**: Model reward configuration as reusable `RewardOption` records with explicit scope (`family` or `child`) and resolve child-visible options as an additive list of active family options plus active child-specific options.

**Rationale**:
- Matches clarified requirement that child options are additive, not override-only.
- Supports both household defaults and child personalization without extra mode toggles.
- Keeps query logic deterministic and easy to test.

**Alternatives considered**:
- Child-only override mode: rejected because it hides useful family defaults.
- Parent-selectable merge mode per child: rejected for added UI complexity with limited benefit.

---

### 2. Multi-Unit Ledger Strategy

**Decision**: Maintain separate balances and ledger streams per reward unit/type (for example, USD, screen-time minutes, console-time minutes).

**Rationale**:
- Prevents invalid arithmetic across unlike units.
- Aligns with clarification decision and supports transparent payout history.
- Simplifies future reporting by unit without conversion assumptions.

**Alternatives considered**:
- Single normalized balance: rejected due to forced conversion policy and fairness disputes.
- Parent-defined conversion table: rejected as over-scoped for current feature.

---

### 3. Page-Based Milestone Earnings

**Decision**: For page-milestone rules, grant earnings only at full threshold completion; carry leftover pages forward to the next threshold.

**Rationale**:
- Matches explicit clarification outcome.
- Produces predictable, audit-friendly calculations.
- Avoids floating-point payout drift from per-page fractions.

**Alternatives considered**:
- Continuous fractional earning: rejected due to complexity in mixed unit ledgers.
- Discard leftover pages on completion: rejected as user-hostile.

---

### 4. Payout/Spend Authorization Workflow

**Decision**: Child initiates payout or spend request; parent approval finalizes ledger state transitions to `paid_out` or `spent`.

**Rationale**:
- Preserves parent control while keeping child agency.
- Produces explicit approval events useful for disputes/auditing.
- Aligns with existing family permission boundaries.

**Alternatives considered**:
- Child self-finalization: rejected due to trust and abuse risk.
- Parent-only initiation without child request: rejected because it hides child intent.

---

### 5. Messaging Channel and Throttling

**Decision**: Support two in-app message intents: child `nudge` and parent `encouragement`; send email only for nudges to parents; enforce one nudge per child per 24 hours.

**Rationale**:
- Meets clarified anti-spam and channel requirements.
- Keeps child communication in-app only, avoiding child email delivery.
- Uses cooldown as the simplest enforceable rate limiter.

**Alternatives considered**:
- No cooldown: rejected due to spam risk.
- Parent-configurable cooldown: rejected for additional UI complexity in v1.

---

### 6. Book Metadata Source for Page Count Suggestions

**Decision**: Use OpenLibrary as the primary metadata provider for page-count suggestions, with always-available manual override persisted per reading assignment.

**Rationale**:
- OpenLibrary is already aligned with the product domain and publicly accessible.
- Suggestion + override handles edition mismatch safely.
- Prevents blocking progress when metadata is unavailable.

**Alternatives considered**:
- Require exact page count before tracking: rejected because it blocks user flow.
- Multiple metadata providers in v1: rejected as unnecessary integration complexity.

---

### 7. Integration Boundaries and Delivery Strategy

**Decision**: Implement as incremental backend and frontend enhancement inside existing modules (no new service split), preserving current auth and role boundaries.

**Rationale**:
- Satisfies constitution parity-first requirement.
- Reduces migration risk and deployment overhead.
- Keeps test updates localized to rewards, reading progress, and messaging surfaces.

**Alternatives considered**:
- Separate rewards microservice: rejected as disproportionate for feature scope.
- Frontend-only derived logic: rejected due to ledger integrity concerns.

---

## Summary of Resolved Clarifications

- Page milestones: block-based with carry-forward leftovers.
- Mixed reward types: separate ledger and balances per unit.
- Payout/spend control: child requests, parent final approval.
- Nudge limit: one per child per 24-hour window.
- Option visibility: additive family + child-specific list.

All previous uncertainty is resolved. No `NEEDS CLARIFICATION` items remain for planning.

---

### 8. Global Selection Fallback vs Per-Book Basis

**Decision**: Retain child global active reward selection only as fallback; explicit per-book basis selection at book start takes precedence for that book.

**Rationale**:
- Preserves existing child selection UX while enabling required per-book control.
- Minimizes migration risk by keeping currently implemented selection primitives.
- Keeps precedence rules deterministic.

**Alternatives considered**:
- Remove global selection entirely: rejected due to migration churn and unnecessary UX disruption.
- Keep global selection as sole control: rejected because it fails clarified requirement for per-book basis choice.

---

### 9. Reward Value Typing for Monetary and Non-Monetary Rewards

**Decision**: Use a typed reward value model: `MONEY` stores currency amount; `NON_MONEY` stores structured quantity + unit label.

**Rationale**:
- Directly supports examples such as TV episodes without fake dollar values.
- Enables consistent validation and ledger grouping semantics.
- Avoids lossy free-text interpretation.

**Alternatives considered**:
- Monetary-only numeric amount with note text for non-money: rejected due to weak structure and poor reporting.
- One generic amount + free-text unit for all options: rejected due to weaker validation and UX ambiguity.

---

### 10. Basis Mutability During In-Progress Books

**Decision**: Lock earning basis per book once selected at book start; basis changes apply only to books started later.

**Rationale**:
- Prevents mid-book rule switching disputes.
- Produces clear auditability for earned events.
- Simplifies testability and state transitions.

**Alternatives considered**:
- Allow immediate mid-book switches: rejected due to inconsistent earning semantics.
- Mid-book switches with parent approval: rejected as additional complexity not required by clarified scope.

---

### 11. Completion-Time Choice Behavior Under Ambiguity

**Decision**: If multiple eligible options exist for a chapter/page completion event, require explicit child choice; block completion/earning until selected.

**Rationale**:
- Eliminates hidden auto-selection behavior.
- Improves fairness and user comprehension.
- Produces deterministic ledger attribution.

**Alternatives considered**:
- Auto-fallback to global selection: rejected because user requested explicit choice requirement.
- Auto-select highest value: rejected because it can conflict with parent intent and unit context.

---

### 12. PER_PAGE Count Source and Earning Gate

**Decision**: Prefill page count from OpenLibrary, but require explicit user confirmation or manual override before any PER_PAGE earning events can post.

**Rationale**:
- Balances convenience with edition-accuracy risk.
- Prevents silent earnings based on unverified metadata.
- Creates auditable setup state before milestone accrual.

**Alternatives considered**:
- User-entered only: rejected due to avoidable data-entry friction.
- Auto-accept metadata without confirmation: rejected due to known edition mismatch risk.
- Chapter-count fallback for PER_PAGE: rejected because it violates basis semantics.

---

### 13. PER_BOOK Tracking Inputs and Eligibility

**Decision**: For PER_BOOK basis, chapter and page counts are optional tracking-only inputs and are never required for earning eligibility.

**Rationale**:
- Aligns with outcome-based earning semantics for book completion.
- Avoids forcing irrelevant setup data.
- Keeps optional progress-tracking flexibility for families that want it.

**Alternatives considered**:
- Require chapter count for PER_BOOK: rejected as unnecessary coupling.
- Require either chapter or page count for PER_BOOK: rejected due to redundant gating.
