# Earnings and Ledger Rules Contract

**Feature**: `004-custom-rewards`

## Rule 1: Unit Separation

- Ledger balances are maintained per `(child, unitType)` pair.
- Transactions from one unit type MUST NOT alter balances for another unit type.

## Rule 2: Additive Option Visibility

- Child offered options = active family-scoped options + active child-scoped options for that child.

## Rule 3: Page Milestone Earning

For a reward option with `earningBasis=PER_PAGE_MILESTONE` and milestone size `M`:

- `milestonesEarnedTotal = floor(totalQualifiedPages / M)`
- New earning events are generated only for delta milestones not previously credited.
- Leftover pages (`totalQualifiedPages mod M`) carry forward.

## Rule 4: Per-Book Completion Earning

- `earningBasis=PER_BOOK` grants earning upon completion signal for the assignment.
- Chapter tracking is optional and not required for payout eligibility.

## Rule 5: Settlement Authorization

- Child can create settlement requests (`PAYOUT` or `SPEND`).
- Parent approval is required to finalize corresponding ledger entry types (`PAID_OUT`, `SPENT`).
- Rejected requests leave balances unchanged.

## Rule 6: Messaging and Notification

- Child `NUDGE` messages may be sent only when at least 24 hours have elapsed since the child's previous nudge.
- Nudge delivery channels: in-app to parent plus parent email notification.
- Parent `ENCOURAGEMENT` messages are in-app only to child; no child email delivery.
