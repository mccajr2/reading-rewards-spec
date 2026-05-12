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
