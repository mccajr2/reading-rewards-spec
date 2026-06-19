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

## Rule 7: Global Selection Fallback Precedence

- Child global active selection is a fallback only.
- Per-book basis selected at book start overrides fallback for that book.
- Explicit completion-time option choice overrides both for that earning event.

## Rule 8: Per-Book Basis Lock

- A reading assignment MUST store earning basis at start.
- Basis MUST remain immutable for the life of the in-progress book.
- Basis updates apply only to newly started books.

## Rule 9: Completion-Time Option Requirement

- For chapter/page completion events with multiple eligible reward options, the child MUST explicitly choose one option.
- If explicit selection is missing, completion/earning is blocked and no ledger entry is posted.

## Rule 10: PER_PAGE Count Confirmation Gate

- OpenLibrary page count is a suggestion only.
- For `PER_PAGE` basis, page-based milestone earnings MUST remain blocked until the user confirms suggested count or submits an override.
- Confirmation state must be persisted and used as a gate for earning event posting.

## Rule 11: PER_BOOK Tracking Optionality

- For `PER_BOOK` basis, chapter and page fields are optional tracking data only.
- `PER_BOOK` earning eligibility is based on completion signal and MUST NOT depend on chapter count or page count presence.
