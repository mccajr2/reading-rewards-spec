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
- child reward selection
- unit-scoped ledgers and ledger entries
- settlement requests
- family messages

2. Implement scope resolution service:
- return additive list: family active + child active options

3. Implement earnings engine updates:
- per chapter
- per completed book
- per page milestone with leftover carry-forward

4. Implement settlement approval flow:
- child creates payout/spend request
- parent approves to finalize ledger movement

5. Implement messaging constraints:
- nudge: child -> parent, in-app + parent email
- encouragement: parent -> child, in-app only
- enforce one nudge per child per 24 hours

## 3. Frontend Implementation Sequence

1. Parent reward management UI:
- family-level reward options
- child-level reward options
- add/edit/deactivate options
- preloaded default `$1 per chapter` option

2. Child rewards UI:
- display additive option list
- select active reward option
- show balances grouped by unit type

3. Reading progress UI enhancements:
- tracking mode per assignment: book/chapter/page
- page count suggestion from OpenLibrary when available
- explicit parent override control for page count

4. Settlement and messaging UI:
- child request payout/spend
- parent approve/reject actions
- nudge and encouragement message composer
- cooldown indicator for nudge availability

## 4. Contracts and Test Alignment

Use contracts in `specs/004-custom-rewards/contracts/` as source-of-truth for API behavior.

Backend tests to add/update:
- reward option scope resolution tests
- earnings calculation tests (chapter/book/page milestones)
- ledger separation tests by unit type
- settlement authorization tests (parent approval required)
- nudge cooldown tests (24-hour enforcement)

Frontend tests to add/update:
- parent reward configuration flows
- child reward selection and display by unit ledger
- payout/spend request approval UX
- messaging behavior (nudge + encouragement channel rules)

E2E smoke coverage:
- parent configures reward options -> child selects -> child logs reading -> child requests payout -> parent approves

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
- Page milestone earning uses threshold-only logic with leftover carry-forward.
- Mixed units remain in separate ledgers and balances.
- Child cannot finalize payout/spend without parent approval.
- Nudge cooldown is enforced at one per child per 24 hours.
- Parent accounts do not show child reward selection/earning workflows.
