# Quickstart: Parent Reward Customization

**Feature**: `004-custom-rewards` | **Date**: 2026-05-12

This guide describes how to implement and validate customizable reward options across family and child scopes while preserving existing auth and reading workflows.

## 1. Prepare Workspace

```bash
cd /Users/jasonmccarthy/projects/reading-rewards-spec
```

Install dependencies for both modules if needed:

```bash
cd backend && ./mvnw -q -DskipTests compile
cd ../frontend && npm install
```

## 2. Backend Implementation Sequence

1. Add/extend domain entities and migrations for:
- reward options (scope + basis + unit)
- typed reward values (`MONEY` vs `NON_MONEY`)
- child reward selection
- per-book basis selection and basis lock
- completion-time option selection events
- unit-scoped ledgers and ledger entries
- settlement requests
- family messages

2. Implement scope resolution service:
- return additive list: family active + child active options

3. Implement earnings engine updates:
- per chapter
- per completed book
- per page milestone with leftover carry-forward
- explicit completion-time option selection requirement when multiple eligible options exist
- block completion/earning if explicit selection is required but missing
- for PER_PAGE basis, block milestone earning until page count is confirmed or overridden
- for PER_BOOK basis, do not gate earning on chapter/page input presence

4. Implement settlement approval flow:
- child creates payout/spend request
- parent approves to finalize ledger movement

5. Implement messaging constraints:
- nudge: child -> parent, in-app + parent email
- encouragement: parent -> child, in-app only
- enforce one nudge per child per 24 hours

## 3. Frontend Implementation Sequence

1. Parent reward management UI:
- dedicated Manage Rewards page separate from Manage Kids
- family-level reward options
- child-level reward options
- add/edit/deactivate options
- preloaded default `$1 per chapter` option
- typed value controls:
	- MONEY -> currency amount
	- NON_MONEY -> quantity + unit label

2. Child rewards UI:
- display additive option list
- select active reward option
- show balances grouped by unit type

3. Reading progress UI enhancements:
- tracking mode per assignment: book/chapter/page
- required per-book basis picker at book start
- prevent mid-book basis change
- page count suggestion from OpenLibrary when available
- explicit parent override control for page count
- explicit page-count confirmation step before PER_PAGE earning begins
- completion-time reward option picker for chapter/page events when multiple options are eligible

4. Settlement and messaging UI:
- child request payout/spend
- parent approve/reject actions
- nudge and encouragement message composer
- cooldown indicator for nudge availability

## 4. Contracts and Test Alignment

Use contracts in `specs/004-custom-rewards/contracts/` as source-of-truth for API behavior.

Backend tests to add/update:
- reward option scope resolution tests
- typed value validation tests (`MONEY`/`NON_MONEY`)
- earnings calculation tests (chapter/book/page milestones)
- per-book basis lock tests
- completion-time selection required/blocking tests
- PER_PAGE page-count confirmation gate tests (no earnings before confirmation)
- PER_BOOK eligibility tests (earn on completion without chapter/page counts)
- ledger separation tests by unit type
- settlement authorization tests (parent approval required)
- nudge cooldown tests (24-hour enforcement)

Frontend tests to add/update:
- parent reward configuration flows
- child reward selection and display by unit ledger
- payout/spend request approval UX
- messaging behavior (nudge + encouragement channel rules)

E2E smoke coverage:
- parent configures typed reward options on dedicated rewards page
- child starts book and selects basis
- child completes chapter/page event and must choose explicit option when multiple match
- child logs reading -> child requests payout -> parent approves

## 5. Verification Commands

Backend:

```bash
cd backend
./mvnw test
```

Frontend:

```bash
cd frontend
npm run test
npm run build
```

Optional E2E:

```bash
npm run test:e2e
```

## 6. Definition of Done for This Feature

- Child-visible options follow additive family + child scope behavior.
- Manage Rewards and Manage Kids are separate parent flows.
- Non-money rewards never require/display dollar amount and use structured quantity/unit fields.
- Book basis is selected at start and cannot be changed mid-book.
- Completion events with multiple eligible options are blocked until explicit child option choice is made.
- PER_PAGE earnings are blocked until metadata page count is confirmed or overridden.
- PER_BOOK earnings do not require chapter/page tracking inputs.
- Page milestone earning uses threshold-only logic with leftover carry-forward.
- Mixed units remain in separate ledgers and balances.
- Child cannot finalize payout/spend without parent approval.
- Nudge cooldown is enforced at one per child per 24 hours.
- Parent accounts do not show child reward selection/earning workflows.
