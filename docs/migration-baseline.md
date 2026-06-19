# Migration Baseline

This document anchors the successor implementation to the behavior of the legacy app.

Legacy source repository:

- `/Users/jasonmccarthy/projects/reading-rewards`

Behavioral pillars to preserve:

- Parent signup, verification, and login.
- Child account creation and username-based login.
- Book search, add, chapter setup, reading progress, and reread flow.
- Reward earning, spending, summaries, and history.
- Parent dashboards and child summaries.

Known explicit changes allowed in the successor:

- Internal architecture can change.
- Frontend styling can change.
- Toolchains and dependencies can be upgraded.
- Secret handling must be cleaned up.

## Feature Additions (Spec 004 — Parent Reward Customization)

Implemented on top of the baseline as of 2026-05-19:

- **Custom Reward Options**: Parents define reward options at family or child scope (MONEY / NON_MONEY), with configurable basis (PER_CHAPTER, PER_BOOK, PER_PAGE) and unit (USD, tokens, custom).
- **Child Reward Selection**: Children can select their active reward option at completion time or in advance; parents can manage on their behalf.
- **Earnings Engine**: Per-chapter, per-book, and per-page-milestone earnings with carry-forward; option selection required when multiple eligible options exist.
- **Settlement Requests**: Children initiate payout/spend requests; parents approve or reject to finalize ledger movements.
- **Family Messaging**: Children send nudges (in-app + parent email, 24-hour cooldown); parents send encouragement (in-app only).
- **Parent Dashboard**: Family reward summary, child reward management, pending settlement queue, and messaging panel.