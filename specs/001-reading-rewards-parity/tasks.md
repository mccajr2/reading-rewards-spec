# Tasks: Reading Rewards Parity Rebuild

**Input**: Design documents from `/specs/001-reading-rewards-parity/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/, quickstart.md

**Baseline Note**: The core app is already implemented and testable. This checklist now tracks incremental alignment and improvement work from the current baseline.

**Tests**: Backend, frontend, and end-to-end tests are required by spec (FR-010, SC-002).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: User story label (`[US1]`, `[US2]`, `[US3]`)
- Every task includes a concrete target file path (existing file or explicitly planned new file)

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Align toolchain, environment templates, and test harnesses for the rebuilt repo.

- [ ] T001 Update environment templates for JWT/Brevo/frontend URL parity in `.env.example`
- [X] T002 Update compose defaults for backend/frontend runtime env alignment in `docker-compose.yml`
- [X] T003 [P] Verify backend build/test toolchain setup for Java 21 in `backend/pom.xml`
- [X] T004 [P] Verify frontend build/test toolchain setup for Vite/Vitest in `frontend/package.json`
- [X] T005 [P] Validate Playwright base URL and project config in `playwright.config.ts`
- [X] T006 [P] Refresh quickstart run/test commands and env prerequisites in `specs/001-reading-rewards-parity/quickstart.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core contracts, security boundaries, and shared domain behavior required by all stories.

**CRITICAL**: No user story work should begin until this phase is complete.

- [X] T007 Normalize parent/child capability rules in API contract notes in `specs/001-reading-rewards-parity/contracts/README.md`
- [X] T008 [P] Add/confirm role boundary checks for parent vs child endpoints in `backend/src/main/java/com/example/readingrewards/auth/config/SecurityConfig.java`
- [X] T009 [P] Ensure JWT secret parsing supports env-safe formats with secure fallback behavior in `backend/src/main/java/com/example/readingrewards/auth/util/JwtUtil.java`
- [ ] T010 [P] Add shared error response mapping for authorization and validation failures in `backend/src/main/java/com/example/readingrewards/domain/controller/ApiController.java`
- [ ] T011 [P] Align shared DTOs for book reads, chapter reads, and rewards rollups in `backend/src/main/java/com/example/readingrewards/domain/dto/BookSummaryDto.java`
- [X] T012 [P] Add frontend auth guard utility for role-based route protection in `frontend/src/app/App.tsx`
- [X] T013 Add route map alignment for parent/child navigation boundaries in `frontend/src/app/App.tsx`

**Checkpoint**: Foundation complete. User stories can now be implemented and tested independently.

---

## Phase 3: User Story 1 - Parent Account Lifecycle And Dashboard (Priority: P1) 🎯 MVP

**Goal**: Parent can sign up/verify/login, manage kids, and view per-child summary cards with drill-down entry points.

**Independent Test**: Sign up and verify a parent, log in, add a child, reset child password, and view dashboard cards with drill-down links for each child.

### Tests for User Story 1

- [X] T014 [P] [US1] Add backend integration tests for parent auth lifecycle and kid management in `backend/src/test/java/com/example/readingrewards/auth/AuthControllerIntegrationTests.java`
- [X] T015 [P] [US1] Add backend integration tests for parent dashboard summary card payloads in `backend/src/test/java/com/example/readingrewards/domain/ApiControllerIntegrationTests.java`
- [X] T016 [P] [US1] Add frontend component tests for signup/login/verify flows in `frontend/src/features/auth/LoginPage.test.tsx`
- [X] T017 [P] [US1] Add frontend component tests for parent dashboard cards and drill-down navigation in `frontend/src/features/parent/ParentDashboard.test.tsx`
- [X] T018 [US1] Add Playwright scenario for parent onboarding, kid creation, and dashboard card visibility in `tests/e2e/parent.spec.ts`

### Implementation for User Story 1

- [X] T019 [P] [US1] Implement parent signup endpoint validation and account creation flow in `backend/src/main/java/com/example/readingrewards/auth/controller/AuthController.java`
- [X] T020 [P] [US1] Implement email verification and token invalidation flow in `backend/src/main/java/com/example/readingrewards/auth/controller/AuthController.java`
- [X] T021 [P] [US1] Implement parent login/logout with verified-account guard in `backend/src/main/java/com/example/readingrewards/auth/controller/AuthController.java`
- [X] T022 [P] [US1] Implement create/list/reset child account endpoints in `backend/src/main/java/com/example/readingrewards/domain/controller/ParentController.java`
- [X] T023 [US1] Implement parent dashboard summary endpoint (per-child rollup cards) in `backend/src/main/java/com/example/readingrewards/domain/controller/ParentController.java`
- [X] T024 [P] [US1] Implement signup screen behavior and validation in `frontend/src/features/auth/SignupPage.tsx`
- [X] T025 [P] [US1] Implement verify-email screen with backend-driven status messaging in `frontend/src/features/auth/VerifyEmailPage.tsx`
- [X] T026 [P] [US1] Implement login screen role-aware flow in `frontend/src/features/auth/LoginPage.tsx`
- [X] T027 [P] [US1] Implement parent auth/session state handling in `frontend/src/features/auth/AuthContext.tsx`
- [X] T028 [US1] Implement parent dashboard with per-child cards and drill-down links in `frontend/src/features/parent/ParentDashboard.tsx`
- [X] T029 [US1] Update parent navigation items to expose dashboard and kid management flows in `frontend/src/features/nav/Nav.tsx`
- [X] T030 [US1] Document US1 endpoint contracts and payload examples in `specs/001-reading-rewards-parity/contracts/README.md`
- [X] T073 [P] [US1] Add backend integration tests for parent self reading-list add/list behavior in `backend/src/test/java/com/example/readingrewards/domain/ApiControllerIntegrationTests.java`
- [X] T074 [P] [US1] Implement parent self reading-list add/list authorization behavior in `backend/src/main/java/com/example/readingrewards/domain/controller/ApiController.java`
- [X] T075 [P] [US1] Add parent self reading-list entry points in the parent dashboard UI in `frontend/src/features/parent/ParentDashboard.tsx`
- [X] T076 [US1] Add Playwright scenario for parent self reading-list positive path and child-boundary denial in `tests/e2e/parent.spec.ts`

**Checkpoint**: User Story 1 is independently functional and testable.

---

## Phase 4: User Story 2 - Child Reading Progress And Rewards (Priority: P1)

**Goal**: Child can search/add books, seed/reuse chapters, mark chapter reads, earn/spend/payout rewards, finish and reread books.

**Independent Test**: Log in as child, add a searched book, seed chapters if missing, mark chapter read, verify rewards, finish and reread successfully.

### Tests for User Story 2

- [X] T031 [P] [US2] Add backend integration tests for search/add/finish/reread workflows in `backend/src/test/java/com/example/readingrewards/domain/ApiControllerIntegrationTests.java`
- [X] T032 [P] [US2] Add backend integration tests for chapter seed/reuse, explicit chapter rename behavior, and chapter-read reward side effects in `backend/src/test/java/com/example/readingrewards/domain/ApiControllerIntegrationTests.java`
- [X] T033 [P] [US2] Add frontend tests for search/add book and chapter seeding prompt flow in `frontend/src/features/books/SearchPage.test.tsx`
- [X] T034 [P] [US2] Add frontend tests for reading list chapter state, chapter rename UX, and reward math display in `frontend/src/features/books/ReadingListPage.test.tsx`
- [X] T035 [P] [US2] Add frontend tests for rewards history and spend/payout actions in `frontend/src/features/rewards/RewardsPage.test.tsx`
- [X] T036 [US2] Add Playwright child journey for add/read/reward/finish/reread flow in `tests/e2e/reading.spec.ts`

### Implementation for User Story 2

- [X] T037 [P] [US2] Implement search provider service using Open Library with resilient mapping in `backend/src/main/java/com/example/readingrewards/domain/service/GoogleBooksService.java`
- [X] T038 [P] [US2] Implement search/books CRUD and finish/reread endpoints in `backend/src/main/java/com/example/readingrewards/domain/controller/ApiController.java`
- [X] T039 [P] [US2] Implement chapter list/create/update (including explicit rename semantics) and bookRead-based chapter endpoints in `backend/src/main/java/com/example/readingrewards/domain/controller/ApiController.java`
- [X] T040 [P] [US2] Implement chapter-read create/delete behavior with reward side effects in `backend/src/main/java/com/example/readingrewards/domain/controller/ApiController.java`
- [X] T041 [P] [US2] Implement rewards summary/history/spend/payout plus credits alias endpoint in `backend/src/main/java/com/example/readingrewards/domain/controller/ApiController.java`
- [X] T042 [US2] Implement chapter seed-on-first-add and reuse-on-next-add domain behavior in `backend/src/main/java/com/example/readingrewards/domain/controller/ApiController.java`
- [X] T043 [P] [US2] Implement child search page UX with robust network error handling in `frontend/src/features/books/SearchPage.tsx`
- [X] T044 [P] [US2] Implement reading list chapter management and read/unread toggles in `frontend/src/features/books/ReadingListPage.tsx`
- [X] T045 [P] [US2] Implement scanner/manual search integration behavior in `frontend/src/features/books/Scanner.tsx`
- [X] T046 [P] [US2] Implement rewards and credits views for child account in `frontend/src/features/rewards/RewardsPage.tsx`
- [X] T047 [US2] Implement child history view wiring and pagination behavior in `frontend/src/features/books/HistoryPage.tsx`
- [X] T048 [US2] Update shared DTO typings for chapter/reward/history responses in `frontend/src/shared/api.ts`
- [X] T049 [US2] Document US2 endpoint contracts and Open Library notes in `specs/001-reading-rewards-parity/contracts/README.md`

**Checkpoint**: User Story 2 is independently functional and testable.

---

## Phase 5: User Story 3 - Parent Child Detail And Reversal Capability (Priority: P2)

**Goal**: Parent can drill into a child detail view, inspect all reading/reward activity, and reverse erroneous chapter-read/reward entries.

**Independent Test**: Parent opens a child detail screen, inspects in-progress/finished books + reward ledger, reverses one chapter-read, and sees both chapter-read and reward reversed.

### Tests for User Story 3

- [X] T050 [P] [US3] Add backend integration tests for parent child-detail payload and authorization boundaries in `backend/src/test/java/com/example/readingrewards/domain/ApiControllerIntegrationTests.java`
- [X] T051 [P] [US3] Add backend integration tests for parent-triggered chapter-read reversal behavior in `backend/src/test/java/com/example/readingrewards/domain/ApiControllerIntegrationTests.java`
- [X] T052 [P] [US3] Add frontend tests for parent child-detail rendering and reward ledger visibility in `frontend/src/features/parent/ParentSummary.test.tsx`
- [X] T053 [P] [US3] Add frontend tests for reversal action UX and post-reversal state refresh in `frontend/src/features/parent/ParentSummary.test.tsx`
- [X] T054 [US3] Add Playwright parent drill-down and reversal scenario in `tests/e2e/parent.spec.ts`

### Implementation for User Story 3

- [X] T055 [P] [US3] Implement parent child-detail endpoint (books, chapters, reward history/ledger, and summary rollups) in `backend/src/main/java/com/example/readingrewards/domain/controller/ParentController.java`
- [X] T056 [P] [US3] Implement parent-triggered reverse chapter-read endpoint and service logic in `backend/src/main/java/com/example/readingrewards/domain/controller/ParentController.java`
- [X] T057 [P] [US3] Enforce parent-only access checks for child-detail and reversal endpoints in `backend/src/main/java/com/example/readingrewards/auth/config/JwtAuthFilter.java`
- [X] T058 [P] [US3] Implement parent summary drill-down page with read-only child activity in `frontend/src/features/parent/ParentSummary.tsx`
- [X] T059 [P] [US3] Implement reversal action controls in parent child-detail page in `frontend/src/features/parent/ParentSummary.tsx`
- [X] T060 [US3] Wire parent child-detail route and guard behavior in `frontend/src/app/App.tsx`
- [X] T061 [US3] Update parent summary page navigation to child detail routes in `frontend/src/features/parent/ParentSummary.tsx`
- [X] T062 [US3] Document US3 contracts (child-detail and reversal) in `specs/001-reading-rewards-parity/contracts/README.md`

**Checkpoint**: User Story 3 backend and frontend implementation plus contracts are in place; frontend and E2E verification tasks (`T052`-`T054`) remain before full sign-off.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Cross-story hardening, cleanup, and validation.

- [X] T063 [P] Remove duplicate/contradictory lines introduced in clarified spec sections in `specs/001-reading-rewards-parity/spec.md`
- [X] T064 [P] Align data model naming from `googleBookId` to provider-agnostic wording notes in `specs/001-reading-rewards-parity/data-model.md`
- [X] T065 [P] Update quickstart with parent dashboard and reversal verification steps in `specs/001-reading-rewards-parity/quickstart.md`
- [X] T066 [P] Add Open Library dependency note and fallback behavior in `README.md`
- [X] T067 Run backend test suite and capture summary results in `specs/001-reading-rewards-parity/quickstart.md`
- [X] T068 Run frontend unit test suite and capture summary results in `specs/001-reading-rewards-parity/quickstart.md`
- [X] T069 Run Playwright E2E suite and capture summary results in `specs/001-reading-rewards-parity/quickstart.md`
- [X] T070 [P] Add backend unit tests for domain reward and chapter-read logic in `backend/src/test/java/com/example/readingrewards/domain/RewardLogicUnitTests.java`
- [X] T071 [P] Add backend unit tests for auth and JWT utility behavior in `backend/src/test/java/com/example/readingrewards/auth/JwtUtilUnitTests.java`
- [X] T072 [P] Add SC-004 isolation/traceability verification checklist with explicit validation commands in `specs/001-reading-rewards-parity/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies; start immediately.
- **Phase 2 (Foundational)**: Depends on Phase 1; blocks all user stories.
- **Phase 3 (US1)**: Depends on Phase 2; MVP target.
- **Phase 4 (US2)**: Depends on Phase 2; can run in parallel with US1 after foundation is complete.
- **Phase 5 (US3)**: Depends on Phase 2 and contracts from US1/US2; should start after US1 dashboard baseline.
- **Phase 6 (Polish)**: Depends on completion of desired user stories.

### User Story Dependencies

- **US1 (P1)**: Independent after foundational phase.
- **US2 (P1)**: Independent after foundational phase.
- **US3 (P2)**: Depends on US1 dashboard drill-down entry points and US2 chapter-read/reward data.

### Within Each User Story

- Tests first (write and confirm fail before implementation).
- Backend/domain changes before UI integration wiring.
- Contracts/docs update before story checkpoint sign-off.

## Parallel Opportunities

- Setup tasks `T003`-`T006` are parallelizable.
- Foundational tasks `T008`-`T012` are parallelizable.
- In US1, backend endpoint tasks `T019`-`T023` and frontend tasks `T024`-`T029` can run in parallel by separate contributors.
- In US2, backend tasks `T037`-`T042` and frontend tasks `T043`-`T048` can run in parallel.
- In US3, endpoint tasks `T055`-`T057` and frontend tasks `T058`-`T061` can run in parallel.

---

## Parallel Example: User Story 1

```bash
# Parallel backend work
Task: T019 Implement parent signup endpoint validation flow in backend/src/main/java/com/example/readingrewards/auth/controller/AuthController.java
Task: T022 Implement child account management endpoints in backend/src/main/java/com/example/readingrewards/domain/controller/ParentController.java

# Parallel frontend work
Task: T024 Implement signup screen behavior in frontend/src/features/auth/SignupPage.tsx
Task: T028 Implement parent dashboard card view in frontend/src/features/parent/ParentDashboard.tsx
```

## Parallel Example: User Story 2

```bash
# Parallel service and UI work
Task: T037 Implement Open Library search mapping in backend/src/main/java/com/example/readingrewards/domain/service/GoogleBooksService.java
Task: T044 Implement reading list chapter toggles in frontend/src/features/books/ReadingListPage.tsx
```

## Parallel Example: User Story 3

```bash
# Parallel parent detail and reversal work
Task: T055 Implement parent child-detail endpoint in backend/src/main/java/com/example/readingrewards/domain/controller/ParentController.java
Task: T058 Implement parent child-detail page in frontend/src/features/parent/ParentSummary.tsx
```

---

## Implementation Strategy

### MVP First (US1)

1. Complete Phase 1 (Setup).
2. Complete Phase 2 (Foundational).
3. Complete Phase 3 (US1).
4. Validate US1 independently before expanding scope.

### Incremental Delivery

1. Deliver US1 (parent auth + dashboard cards + drill-down links).
2. Deliver US2 (child reading/rewards core behavior).
3. Deliver US3 (parent detail view + reversal controls).
4. Finish with polish, documentation, and full-suite validation.

### Parallel Team Strategy

1. Team aligns on Setup + Foundational tasks.
2. Split by story after foundation is complete:
   - Developer A: US1
   - Developer B: US2
   - Developer C: US3 (after US1 drill-down baseline)
3. Reintegrate at story checkpoints with test evidence.
