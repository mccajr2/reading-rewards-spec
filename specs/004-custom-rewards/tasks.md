---
description: "Task list for Parent Reward Customization (Feature 004)"
---

# Tasks: Parent Reward Customization

**Input**: Design documents from `/specs/004-custom-rewards/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 Create backend/domain structure for reward options, ledgers, selection, settlement, and messaging in backend/src/main/java/[...]/domain/
- [X] T002 [P] Create frontend/component structure for reward config, selection, ledger, and messaging in frontend/src/components/
- [X] T003 [P] Install/verify backend dependencies (Java 21, Spring Boot 3.5.6, Flyway, PostgreSQL driver) in backend/pom.xml
- [X] T004 [P] Install/verify frontend dependencies (TypeScript 5.9, React 19, Vite 7, Tailwind, shadcn/ui, Radix UI) in frontend/package.json
- [X] T005 [P] Setup backend test structure in backend/src/test/java/[...]/
- [X] T006 [P] Setup frontend test structure in frontend/src/tests/

---

## Phase 2: Foundational (Blocking Prerequisites)

- [X] T007 Implement database schema migrations for reward options, selection, ledgers, settlement, and messaging in backend/src/main/resources/db/migration/
- [X] T008 [P] Implement base RewardOption, ChildRewardSelection, RewardLedger, RewardLedgerEntry, RewardSettlementRequest, FamilyMessage entities in backend/src/main/java/[...]/domain/
- [X] T009 [P] Implement DTOs for all entities in frontend/src/types/
- [X] T010 [P] Implement OpenAPI contract stubs for reward customization in backend/src/main/java/[...]/web/ and frontend/src/services/
- [X] T011 [P] Configure API routing for new endpoints in backend/src/main/java/[...]/web/
- [X] T012 [P] Configure API clients for reward, ledger, settlement, and messaging in frontend/src/services/
- [X] T013 [P] Setup environment configuration for new backend/frontend features

---

## Phase 3: User Story 1 - Configure Reward Rules (Priority: P1) 🎯 MVP

**Goal**: Allow parents to define reward rules at family and child scope, with default and override options.

**Independent Test**: Create family-level defaults, add per-child overrides, verify saved options appear for each child.

- [X] T014 [P] [US1] Backend: Implement RewardOption CRUD endpoints in backend/src/main/java/[...]/web/
- [X] T015 [P] [US1] Backend: Implement scope resolution service for additive reward options in backend/src/main/java/[...]/service/
- [X] T016 [P] [US1] Backend: Implement validation for scope, uniqueness, and page milestone rules in backend/src/main/java/[...]/domain/
- [X] T017 [P] [US1] Frontend: Implement parent reward management UI (family/child options, add/edit/deactivate) in frontend/src/components/ParentRewardConfig.tsx
- [X] T018 [P] [US1] Frontend: Implement default $1/chapter option preload in frontend/src/components/ParentRewardConfig.tsx
- [X] T019 [P] [US1] Frontend: Integrate reward option API in frontend/src/services/rewardOptions.ts
- [X] T020 [US1] Backend: Add tests for RewardOption CRUD, scope resolution, and validation in backend/src/test/java/[...]/
- [X] T021 [US1] Frontend: Add tests for reward config UI and API integration in frontend/src/tests/

---

## Phase 4: User Story 2 - Select and Earn Rewards (Priority: P1)

**Goal**: Allow children to select a reward option and earn toward it as they log reading progress.

**Independent Test**: Select reward, log reading, verify earnings accumulate using selected rule.

- [X] T022 [P] [US2] Backend: Implement ChildRewardSelection endpoints and logic in backend/src/main/java/[...]/web/ and service/
- [X] T023 [P] [US2] Backend: Update earnings engine for selected reward rule in backend/src/main/java/[...]/service/
- [X] T024 [P] [US2] Backend: Implement ledger separation by unit type in backend/src/main/java/[...]/domain/
- [X] T025 [P] [US2] Frontend: Implement child reward selection UI in frontend/src/components/ChildRewardSelection.tsx
- [X] T026 [P] [US2] Frontend: Show balances grouped by unit type in frontend/src/components/ChildRewardBalances.tsx
- [X] T027 [P] [US2] Frontend: Integrate selection and ledger APIs in frontend/src/services/
- [X] T028 [US2] Backend: Add tests for selection, earnings, and ledger separation in backend/src/test/java/[...]/
- [X] T029 [US2] Frontend: Add tests for selection UI and ledger display in frontend/src/tests/

---

## Phase 5: User Story 3 - Track Reading Progress by Book, Chapter, or Pages (Priority: P2)

**Goal**: Allow flexible reading progress tracking for per-book, per-chapter, and per-page milestone rewards.

**Independent Test**: Create per-book and chapter/page-based rewards, log progress, verify earnings update correctly.

- [ ] T030 [P] [US3] Backend: Update ReadingAssignment and progress logic for all tracking modes in backend/src/main/java/[...]/domain/ and service/
- [ ] T031 [P] [US3] Backend: Implement page milestone earning logic with carry-forward in backend/src/main/java/[...]/service/
- [ ] T032 [P] [US3] Backend: Integrate OpenLibrary page suggestion and manual override in backend/src/main/java/[...]/service/
- [ ] T033 [P] [US3] Frontend: Update reading progress UI for all tracking modes in frontend/src/components/ReadingProgress.tsx
- [ ] T034 [P] [US3] Frontend: Add parent override for page count in frontend/src/components/ReadingProgress.tsx
- [ ] T035 [US3] Backend: Add tests for progress, milestone, and override logic in backend/src/test/java/[...]/
- [ ] T036 [US3] Frontend: Add tests for reading progress UI and override in frontend/src/tests/

---

## Phase 6: User Story 4 - Manage Payouts and Messages (Priority: P2)

**Goal**: Enable payout/spend requests, parent approval, and in-app messaging with nudge/encouragement logic.

**Independent Test**: Earn unpaid rewards, send nudges, send encouragement, record payout/spend actions.

- [ ] T037 [P] [US4] Backend: Implement RewardSettlementRequest endpoints and approval logic in backend/src/main/java/[...]/web/ and service/
- [ ] T038 [P] [US4] Backend: Implement FamilyMessage endpoints and nudge/encouragement logic in backend/src/main/java/[...]/web/ and service/
- [ ] T039 [P] [US4] Backend: Enforce nudge cooldown and email notification in backend/src/main/java/[...]/service/
- [ ] T040 [P] [US4] Frontend: Implement payout/spend request UI in frontend/src/components/ChildPayoutRequest.tsx
- [ ] T041 [P] [US4] Frontend: Implement nudge/encouragement message UI in frontend/src/components/FamilyMessages.tsx
- [ ] T042 [P] [US4] Frontend: Integrate settlement and messaging APIs in frontend/src/services/
- [ ] T043 [US4] Backend: Add tests for settlement, messaging, and cooldown logic in backend/src/test/java/[...]/
- [ ] T044 [US4] Frontend: Add tests for payout, nudge, and encouragement UI in frontend/src/tests/

---

## Final Phase: Polish & Cross-Cutting Concerns

- [ ] T045 [P] Documentation updates in docs/
- [ ] T046 Code cleanup and refactoring across backend/frontend
- [ ] T047 Performance optimization for reward, ledger, and messaging flows
- [ ] T048 [P] Additional unit tests in backend/src/test/java/[...]/ and frontend/src/tests/
- [ ] T049 Security hardening for new endpoints and UI
- [ ] T050 Run quickstart.md validation steps

---

## Scope Alignment Addendum (Spec Delta from UX Feedback)

- [X] T051 [US1] Frontend: Split parent flows so Manage Rewards is a dedicated page and remove reward configuration UI from kid-management page
- [X] T052 [US1] Backend/Frontend: Add reward value model (monetary vs non-monetary) and enforce unit-aware validation/display (no dollar amount for non-monetary rewards)
- [X] T053 [US2] Backend: Require per-book basis selection (per chapter, per book, per page milestone) at book start before progress events can earn rewards
- [X] T054 [US2] Backend/Frontend: Support completion-time reward choice when basis is per chapter or per page milestone and multiple eligible options are available
- [X] T055 [US2] Backend/Frontend: Add integration tests for start-of-book basis selection and completion-time option selection behavior

---

## Dependencies & Execution Order

- **Setup (Phase 1)**: No dependencies
- **Foundational (Phase 2)**: Blocks all user stories
- **User Stories (Phases 3-6)**: Can proceed in parallel after Foundational
- **Polish (Final Phase)**: After all stories complete

## Parallel Execution Examples

- All [P] tasks can be run in parallel
- User stories can be implemented/tested independently
- Backend and frontend tasks for each story can be parallelized

## MVP Scope

- Complete all tasks in Phase 3 (User Story 1)

## Independent Test Criteria

- Each user story phase includes a test task for backend and frontend
- All test tasks are independently executable

---

# END
