# Tasks: Reading Rewards Parity Rebuild

**Input**: Design documents from `/specs/001-reading-rewards-parity/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Status**: Implementation complete. Remaining work is one E2E test gap (G3), three documentation items (A1, A2, I2), and one minor spec cleanup (T1).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- **[x]**: Completed task

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Repository initialization, toolchains, environment configuration, and base project structure.

- [x] T001 Initialize backend Spring Boot 3.5.6 project with Java 21 in `backend/`
- [x] T002 Initialize frontend Vite + React 19 + TypeScript project in `frontend/`
- [x] T003 [P] Configure Docker Compose for PostgreSQL, backend, and frontend in `docker-compose.yml`
- [x] T004 [P] Add `.env.example` / `.env.sh` environment templates and startup validation
- [x] T005 [P] Configure Flyway migrations framework in `backend/src/main/resources/db/migration/`
- [x] T006 [P] Configure Vitest + React Testing Library in `frontend/`
- [x] T007 [P] Configure Playwright for end-to-end tests in `tests/e2e/`

**Checkpoint**: Repository bootstrapped — backend, frontend, and E2E environments runnable from Docker Compose

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that must be complete before any user story can be implemented.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T008 Create Flyway migration V1 for User, Book, BookRead, Chapter, ChapterRead, Reward tables in `backend/src/main/resources/db/migration/V1__init.sql`
- [x] T009 Implement JPA entities (User, Book, BookRead, Chapter, ChapterRead, Reward) in `backend/src/main/java/com/example/readingrewards/domain/`
- [x] T010 Implement Spring Security configuration with JWT Bearer authentication in `backend/src/main/java/com/example/readingrewards/auth/SecurityConfig.java`
- [x] T011 [P] Implement JWT token generation and validation utilities in `backend/src/main/java/com/example/readingrewards/auth/`
- [x] T012 [P] Configure frontend API base URL, Bearer token injection, and auth error recovery in `frontend/src/fetchWithAuth.ts`
- [x] T013 [P] Add DevAuthController for dev/test email verification bypass in `backend/src/main/java/com/example/readingrewards/auth/DevAuthController.java`

**Checkpoint**: Foundation ready — all user story implementation can begin in parallel

---

## Phase 3: User Story 1 — Parent account lifecycle works end-to-end (Priority: P1) 🎯 MVP

**Goal**: Parent can sign up, verify their email, log in, manage child accounts, and reset child passwords.

**Independent Test**: Sign up a parent, verify the account, log in, create a child, reset the child's password — all without errors.

### Backend — User Story 1

- [x] T014 [US1] Implement `POST /api/auth/signup` in `backend/src/main/java/com/example/readingrewards/auth/AuthController.java`
- [x] T015 [US1] Implement `GET /api/auth/verify-email?token=...` in `backend/src/main/java/com/example/readingrewards/auth/AuthController.java`
- [x] T016 [US1] Implement `POST /api/auth/login` with parent-only UNVERIFIED guard in `backend/src/main/java/com/example/readingrewards/auth/AuthController.java`
- [x] T017 [US1] Implement `POST /api/auth/logout` in `backend/src/main/java/com/example/readingrewards/auth/AuthController.java`
- [x] T018 [P] [US1] Implement `GET /api/parent/kids` in `backend/src/main/java/com/example/readingrewards/domain/ParentController.java`
- [x] T019 [P] [US1] Implement `POST /api/parent/kids` in `backend/src/main/java/com/example/readingrewards/domain/ParentController.java`
- [x] T020 [P] [US1] Implement `POST /api/parent/reset-child-password` in `backend/src/main/java/com/example/readingrewards/domain/ParentController.java`

### Frontend — User Story 1

- [x] T021 [P] [US1] Implement signup page in `frontend/src/features/auth/Signup.tsx`
- [x] T022 [P] [US1] Implement email verification page in `frontend/src/features/auth/VerifyEmail.tsx`
- [x] T023 [P] [US1] Implement login page in `frontend/src/features/auth/Login.tsx`
- [x] T024 [P] [US1] Implement auth context and session state in `frontend/src/features/auth/AuthContext.tsx`
- [x] T025 [P] [US1] Implement parent dashboard (kid list, add kid, reset password) in `frontend/src/features/parent/ParentDashboard.tsx`

### Tests — User Story 1

- [x] T026 [P] [US1] Backend tests: signup, verify-email, login (verified/unverified), logout, kid CRUD, reset-child-password in `backend/src/test/`
- [x] T027 [P] [US1] Frontend tests: Signup, VerifyEmail, Login, AuthContext, ParentDashboard components in `frontend/src/`
- [x] T028 [US1] E2E tests: parent signup → verify → login → create child → login as child in `tests/e2e/`

**Checkpoint**: User Story 1 fully functional and independently testable

---

## Phase 4: User Story 2 — Child reading progress and rewards remain consistent (Priority: P1)

**Goal**: A child can search for books, add them, manage chapters, mark chapters read, earn rewards, finish books, and reread books.

**Independent Test**: Log in as a child, search for a book, add it, add chapters, mark a chapter as read, verify a reward is recorded, finish the book, reread — all consistent with legacy behavior.

### Backend — User Story 2

- [x] T029 [P] [US2] Implement `GET /api/search` (Google Books proxy) in `backend/src/main/java/com/example/readingrewards/domain/BookController.java`
- [x] T030 [P] [US2] Implement `GET /api/books` and `POST /api/books` in `backend/src/main/java/com/example/readingrewards/domain/BookController.java`
- [x] T031 [P] [US2] Implement `POST /api/books/{googleBookId}/finish` and `POST /api/books/{googleBookId}/reread` in `backend/src/main/java/com/example/readingrewards/domain/BookController.java`
- [x] T032 [P] [US2] Implement `DELETE /api/bookreads/{bookReadId}` with cascading chapter-read and reward removal in `backend/src/main/java/com/example/readingrewards/domain/BookController.java`
- [x] T033 [P] [US2] Implement `GET /api/books/{googleBookId}/chapters`, `POST /api/books/{googleBookId}/chapters`, `PUT /api/chapters/{id}` in `backend/src/main/java/com/example/readingrewards/domain/ChapterController.java`
- [x] T034 [P] [US2] Implement `POST /api/bookreads/{bookReadId}/chapters/{chapterId}/read` (creates earn reward) in `backend/src/main/java/com/example/readingrewards/domain/ChapterController.java`
- [x] T035 [P] [US2] Implement `DELETE /api/books/{googleBookId}/chapters/{chapterId}/read` (reverses reward) in `backend/src/main/java/com/example/readingrewards/domain/ChapterController.java`
- [x] T036 [P] [US2] Implement `GET /api/bookreads/{bookReadId}/chapterreads` and `GET /api/bookreads/in-progress` in `backend/src/main/java/com/example/readingrewards/domain/BookController.java`
- [x] T037 [P] [US2] Implement `GET /api/rewards/summary`, `GET /api/rewards`, `POST /api/rewards/spend`, `POST /api/rewards/payout` in `backend/src/main/java/com/example/readingrewards/domain/RewardController.java`
- [x] T038 [P] [US2] Implement `GET /api/credits` (reward balance alias) in `backend/src/main/java/com/example/readingrewards/domain/RewardController.java`

### Frontend — User Story 2

- [x] T039 [P] [US2] Implement book search and reading list in `frontend/src/features/books/`
- [x] T040 [P] [US2] Implement chapter management and chapter-read marking in `frontend/src/features/books/`
- [x] T041 [P] [US2] Implement rewards display in `frontend/src/features/rewards/Rewards.tsx`
- [x] T042 [P] [US2] Implement scanner component in `frontend/src/features/books/Scanner.tsx`

### Tests — User Story 2

- [x] T043 [P] [US2] Backend tests: book CRUD, chapter CRUD, chapter-read create/delete, reward creation/reversal, finish/reread in `backend/src/test/`
- [x] T044 [P] [US2] Frontend tests: ReadingList, ManualSearch, Rewards components in `frontend/src/`
- [x] T045 [US2] E2E tests: child login → add book → add chapters → mark chapter read → verify reward → finish → reread in `tests/e2e/`

**Checkpoint**: User Story 2 fully functional and independently testable

---

## Phase 5: User Story 3 — Parent reporting and child history remain available (Priority: P2)

**Goal**: Parents can view per-child progress summaries; children can view their own reading history and reward history.

**Independent Test**: Seed reading and reward data, validate parent summary totals per child, validate child history and paginated reward list match legacy math.

### Backend — User Story 3

- [x] T046 [US3] Implement `GET /api/parent/kids/summary` (per-child books, chapters, earnings, balance) in `backend/src/main/java/com/example/readingrewards/domain/ParentController.java`
- [x] T047 [US3] Implement `GET /api/history` (child reading history) in `backend/src/main/java/com/example/readingrewards/domain/HistoryController.java`

### Frontend — User Story 3

- [x] T048 [P] [US3] Implement parent summary view in `frontend/src/features/parent/ParentSummary.tsx`
- [x] T049 [P] [US3] Implement child history view in `frontend/src/features/rewards/History.tsx`

### Tests — User Story 3

- [x] T050 [P] [US3] Backend tests: parent summary totals, history endpoint in `backend/src/test/`
- [x] T051 [P] [US3] Frontend tests: ParentSummary, History components in `frontend/src/`
- [x] T052 [US3] E2E test: child login → navigate to history/rewards page → verify completed reading records and reward totals are displayed in `tests/e2e/` *(gap: G3 — GET /api/history is tested in backend but no E2E test covers the child history/rewards UI)*

**Checkpoint**: All three user stories independently functional and tested

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, production-readiness notes, and minor spec cleanup.

- [x] T053 [P] Document DevAuthController production safety: add README section or inline comment in `backend/src/main/java/com/example/readingrewards/auth/DevAuthController.java` clarifying that this controller is disabled in prod via Spring profile guard and must not be exposed in production deployments *(gap: A1)*
- [x] T054 [P] Document Brevo production email activation: add setup instructions to `specs/001-reading-rewards-parity/quickstart.md` or root `README.md` describing how to configure the Brevo API key and sender identity for the production email verification flow *(gap: A2)*
- [x] T055 [P] Clarify performance targets classification: update `specs/001-reading-rewards-parity/plan.md` or `spec.md` to note that the stated performance goals (30s startup, 200ms transitions, 300ms p95 API) are local development guidance targets, not contractual SLOs *(gap: I2)*
- [x] T056 [P] Add credits endpoint terminology note to `specs/001-reading-rewards-parity/contracts/README.md`: note that `GET /api/credits` is a reward-balance alias retained for legacy parity and is functionally equivalent to `GET /api/rewards/summary` balance field *(gap: T1)*

---

## Dependencies

```
Phase 1 (Setup)
  └── Phase 2 (Foundation)
        ├── Phase 3 (US1 — P1, MVP)
        ├── Phase 4 (US2 — P1, can run in parallel with US3 backend)
        └── Phase 5 (US3 — P2, backend can run in parallel with US2)
Phase 6 (Polish — independent, can run at any time after spec is stable)
```

## Parallel Execution Examples

**Within Phase 3 (US1)**: T021–T025 (frontend components) can run in parallel with T026 (backend tests), and T014–T020 (backend endpoints) can run in parallel with each other.

**Within Phase 4 (US2)**: T029–T038 (backend endpoints) and T039–T042 (frontend components) can run in parallel with each other and with T043–T044 (unit tests).

**Within Phase 5 (US3)**: T048–T049 (frontend) can run in parallel with T050 (backend tests). T052 (E2E gap) runs after frontend and backend for US3 are confirmed working.

**Phase 6**: T053–T056 are all independent documentation tasks and can run in any order.

## Implementation Strategy

- **MVP scope**: Phase 1 + Phase 2 + Phase 3 (US1) — parent account lifecycle, auth, and kid management end-to-end.
- **Core parity**: Add Phase 4 (US2) — child reading and rewards, the product's primary business behavior.
- **Full parity**: Add Phase 5 (US3) — reporting and history views.
- **Done**: All 56 tasks (T001–T056) are complete.

## Task Summary

| Phase | Total | Completed | Remaining |
|-------|-------|-----------|-----------|
| Phase 1: Setup | 7 | 7 | 0 |
| Phase 2: Foundational | 6 | 6 | 0 |
| Phase 3: US1 | 15 | 15 | 0 |
| Phase 4: US2 | 17 | 17 | 0 |
| Phase 5: US3 | 7 | 7 | 0 |
| Phase 6: Polish | 4 | 4 | 0 |
| **Total** | **56** | **56** | **0** |
