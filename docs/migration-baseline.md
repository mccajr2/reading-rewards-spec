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