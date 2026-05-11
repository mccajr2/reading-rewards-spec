# Accessibility Audit Checklist - Parent Guidance (Phase 4)

Date: 2026-05-11
Scope: Parent guidance content on /parent, /parent/summary, and /rewards

## Checklist

- [x] Guidance blocks use semantic section structure with readable heading hierarchy
- [x] Guidance blocks expose explicit accessible names via `aria-label`
- [x] Guidance copy is present and descriptive for first-time parent users
- [x] Guidance headings are asserted in automated tests
- [x] Guidance regions are asserted in automated tests
- [ ] Manual VoiceOver pass completed end-to-end (recommended follow-up in browser)

## Evidence

1. Parent dashboard guidance
- Component: `frontend/src/features/parent/ParentDashboard.tsx`
- Accessible guidance label pattern: `Your Dashboard page guidance`
- Test evidence: `frontend/src/features/parent/ParentDashboard.test.tsx`

2. Child account management guidance
- Component: `frontend/src/features/parent/ParentSummary.tsx`
- Accessible guidance labels:
  - `Manage Child Accounts page guidance` (summary)
  - `Child Account Details page guidance` (detail)
- Test evidence: `frontend/src/features/parent/ParentSummary.test.tsx`

3. Rewards settings guidance
- Component: `frontend/src/features/rewards/RewardsPage.tsx`
- Accessible guidance label pattern: `Rewards Settings page guidance`
- Test evidence: `frontend/src/features/rewards/RewardsPage.test.tsx`

4. Validation command
- Executed in `frontend/`:
  - `npm run test -- --poolOptions.threads.singleThread`
  - `npm run build`
- Result: all unit tests passed and production build completed.

## Notes

- This checklist verifies semantic/ARIA coverage and automated assertions.
- Complete a manual VoiceOver pass before release sign-off for full assistive-technology validation.
