# Accessibility Audit Checklist - Child Guidance (Phase 5)

Date: 2026-05-11
Scope: Child guidance content on /reading-list, /search, /history, and /rewards

## Checklist

- [x] Guidance sections use semantic headings and readable structure
- [x] Guidance sections expose explicit accessible names via `aria-label`
- [x] Child guidance language is encouraging and age-appropriate (7-12)
- [x] Guidance headings and regions are covered by automated tests
- [ ] Manual screen reader pass completed with VoiceOver (recommended final verification)

## Evidence

1. Reading list guidance
- Component: `frontend/src/features/books/ReadingListPage.tsx`
- Guidance: `Your Reading List 📚`
- Test evidence: `frontend/src/features/books/ReadingListPage.test.tsx`

2. Search guidance
- Component: `frontend/src/features/books/SearchPage.tsx`
- Guidance: `Find Your Next Book 🔍`
- Test evidence: `frontend/src/features/books/SearchPage.test.tsx`

3. Reading progress/history guidance
- Component: `frontend/src/features/books/HistoryPage.tsx`
- Guidance: `Log Your Reading 📖`
- Test evidence: `frontend/src/features/books/HistoryPage.test.tsx`
- E2E expectation updated in: `tests/e2e/history.spec.ts`

4. Rewards shop guidance
- Component: `frontend/src/features/rewards/RewardsPage.tsx`
- Guidance (child role): `Your Rewards Shop 🎁`
- Test evidence: `frontend/src/features/rewards/RewardsPage.test.tsx`

5. Validation commands
- Executed in `frontend/`:
  - `npm run test -- --poolOptions.threads.singleThread`
  - `npm run build`
- Result: all unit tests passed and production build completed.

## Notes

- The rewards page guidance is role-aware to preserve parent wording from Phase 4 and child wording from Phase 5.
- Complete a manual VoiceOver pass before release sign-off for full assistive-technology validation.
