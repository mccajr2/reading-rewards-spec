# Tasks: Customizable Family Rewards

**Input**: Design documents from `/specs/004-reward-customization/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/parent-reward-api.md, contracts/child-reward-api.md, quickstart.md

**Tests**: Tests are required for this feature because the plan defines tests as delivery criteria and the spec includes explicit independent test scenarios per story.

**Organization**: Tasks are grouped by user story so each story can be implemented and validated independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Task can run in parallel (different files, no dependency on incomplete tasks)
- **[Story]**: User story label (`[US1]` ... `[US10]`) for story-phase tasks only
- Every task includes an explicit file path

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize feature scaffolding for backend, frontend, contracts, and test harnesses.

- [X] T001 Create Flyway migration shell for reward tables in backend/src/main/resources/db/migration/V20260511_01__reward_customization_tables.sql
- [X] T002 Create Flyway migration shell for BookRead reward columns in backend/src/main/resources/db/migration/V20260511_02__bookread_reward_links.sql
- [X] T003 [P] Create reward domain package structure with package-info docs in backend/src/main/java/com/example/readingrewards/domain/model/reward/package-info.java
- [X] T004 [P] Create reward API service stub for frontend integration in frontend/src/services/rewardApi.ts
- [X] T005 [P] Create messaging API service stub for frontend integration in frontend/src/services/messageApi.ts
- [X] T006 [P] Create feature E2E spec shell for reward customization journey in tests/e2e/reward-customization.spec.ts
- [X] T007 [P] Document task-to-contract mapping baseline in specs/004-reward-customization/contracts/README.md

**Checkpoint**: Setup complete; codebase has feature scaffolding for DB, backend domain, frontend services, and tests.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implement core entities, repositories, shared services, and role-aware routing required by all user stories.

**CRITICAL**: No user story implementation starts before this phase is complete.

- [X] T008 Implement RewardTemplate entity and validation rules in backend/src/main/java/com/example/readingrewards/domain/model/reward/RewardTemplate.java
- [X] T009 [P] Implement RewardSelection entity with locked snapshot fields in backend/src/main/java/com/example/readingrewards/domain/model/reward/RewardSelection.java
- [X] T010 [P] Implement RewardAccumulation ledger entity in backend/src/main/java/com/example/readingrewards/domain/model/reward/RewardAccumulation.java
- [X] T011 [P] Implement ProgressTracking entity for chapter/page tracking in backend/src/main/java/com/example/readingrewards/domain/model/reward/ProgressTracking.java
- [X] T012 [P] Implement Message entity for payout reminders and encouragement in backend/src/main/java/com/example/readingrewards/domain/model/message/Message.java
- [X] T013 [P] Add reward repositories for template/selection/accumulation/tracking in backend/src/main/java/com/example/readingrewards/domain/repo/reward/RewardTemplateRepository.java
- [X] T014 [P] Add message repository and query methods in backend/src/main/java/com/example/readingrewards/domain/repo/message/MessageRepository.java
- [X] T015 Implement shared reward calculation engine and formatters in backend/src/main/java/com/example/readingrewards/domain/service/reward/RewardCalculationService.java
- [X] T016 Implement parent/child authorization guard helpers for reward endpoints in backend/src/main/java/com/example/readingrewards/security/RewardAccessPolicy.java
- [X] T017 Implement shared reward DTO set aligned to contracts in backend/src/main/java/com/example/readingrewards/domain/dto/reward/RewardDtos.java
- [X] T018 [P] Add frontend reward route guards for parent vs child page separation in frontend/src/components/AuthContext.tsx
- [X] T019 [P] Add reward route wiring and placeholders in frontend/src/App.tsx
- [X] T020 [P] Add foundational backend integration test base for reward APIs in backend/src/test/java/com/example/readingrewards/integration/reward/RewardApiIntegrationBaseTest.java

**Checkpoint**: Foundation complete; all stories can now build on stable data model, auth policy, and shared APIs.

---

## Phase 3: User Story 1 - Parent Configures Global Reward Defaults (Priority: P1) 🎯 MVP

**Goal**: Parent can create, list, update, and archive family-level reward templates.

**Independent Test**: Parent saves family rewards and all children inherit availability without per-child setup.

### Tests for User Story 1

- [X] T021 [P] [US1] Add parent rewards contract test for GET/POST/PUT/DELETE endpoints in backend/src/test/java/com/example/readingrewards/contract/ParentRewardApiContractTest.java
- [X] T022 [P] [US1] Add frontend component test for global reward settings form in frontend/src/components/parent/__tests__/RewardConfiguration.test.tsx

### Implementation for User Story 1

- [X] T023 [US1] Implement parent reward CRUD service methods for family scope in backend/src/main/java/com/example/readingrewards/domain/service/reward/ParentRewardConfigService.java
- [X] T024 [US1] Implement parent rewards controller endpoints for family rewards in backend/src/main/java/com/example/readingrewards/domain/controller/ParentRewardController.java
- [X] T025 [US1] Implement soft-delete archive behavior and active filter logic in backend/src/main/java/com/example/readingrewards/domain/service/reward/RewardTemplateArchiveService.java
- [X] T026 [US1] Build parent global reward configuration page UI in frontend/src/pages/ParentRewards/ManageFamilyRewardsPage.tsx
- [X] T027 [US1] Build reusable reward template editor component in frontend/src/components/parent/RewardTemplateBuilder.tsx
- [X] T028 [US1] Wire global reward CRUD calls and optimistic updates in frontend/src/services/rewardApi.ts

**Checkpoint**: Parent can manage global reward defaults end-to-end with archived rewards excluded from new selections.

---

## Phase 4: User Story 2 - Parent Creates Per-Child Custom Rewards (Priority: P1)

**Goal**: Parent can create child-specific reward overrides while preserving global defaults.

**Independent Test**: Child A and Child B receive different override sets, each independent from family defaults.

### Tests for User Story 2

- [X] T029 [P] [US2] Add contract test for POST /api/parent/rewards/child/{childId} and list grouping in backend/src/test/java/com/example/readingrewards/contract/ParentPerChildRewardContractTest.java
- [X] T030 [P] [US2] Add frontend test for per-child reward grouping and hierarchy labels in frontend/src/components/parent/__tests__/PerChildOverrides.test.tsx

### Implementation for User Story 2

- [X] T031 [US2] Implement per-child override service with family ownership checks in backend/src/main/java/com/example/readingrewards/domain/service/reward/PerChildRewardService.java
- [X] T032 [US2] Extend parent controller to support per-child override creation and retrieval in backend/src/main/java/com/example/readingrewards/domain/controller/ParentRewardController.java
- [X] T033 [US2] Build per-child override management UI section in frontend/src/components/parent/PerChildOverrides.tsx
- [X] T034 [US2] Add per-child reward API methods and typed responses in frontend/src/services/rewardApi.ts
- [X] T035 [US2] Render parent management hierarchy (family vs child groups) in frontend/src/pages/ParentRewards/ManageFamilyRewardsPage.tsx

**Checkpoint**: Parent can manage child-specific rewards without affecting siblings or family defaults.

---

## Phase 5: User Story 3 - Parent Defines Canned Reward Options (Priority: P1)

**Goal**: Parent can quickly apply built-in reward templates.

**Independent Test**: Parent selects canned templates and children can pick them immediately.

### Tests for User Story 3

- [X] T036 [P] [US3] Add backend test validating all required canned templates exist in backend/src/test/java/com/example/readingrewards/unit/reward/CannedRewardTemplateTest.java
- [X] T037 [P] [US3] Add frontend test for canned template catalog rendering in frontend/src/components/parent/__tests__/CannedRewardCatalog.test.tsx

### Implementation for User Story 3

- [X] T038 [US3] Implement canned reward seed/bootstrap provider in backend/src/main/java/com/example/readingrewards/domain/service/reward/CannedRewardTemplateProvider.java
- [X] T039 [US3] Expose canned template retrieval endpoint for parent UI in backend/src/main/java/com/example/readingrewards/domain/controller/ParentRewardController.java
- [X] T040 [US3] Build canned reward catalog component with one-click add actions in frontend/src/components/parent/CannedRewardCatalog.tsx
- [X] T041 [US3] Integrate canned template selection into parent rewards workflow in frontend/src/pages/ParentRewards/ManageFamilyRewardsPage.tsx

**Checkpoint**: Canned templates are visible, selectable, and saved as active family rewards.

---

## Phase 6: User Story 4 - Child Selects Personal Reward Target (Priority: P1)

**Goal**: Child selects active reward for each book and can update selection mid-book with locked-rate behavior.

**Independent Test**: Child selects reward, receives locked amount/unit snapshot, and reward changes apply only to future progress.

### Tests for User Story 4

- [X] T042 [P] [US4] Add contract test for child reward availability and selection endpoints in backend/src/test/java/com/example/readingrewards/contract/ChildRewardSelectionContractTest.java
- [X] T043 [P] [US4] Add frontend component test for reward selection panel in frontend/src/components/rewards/__tests__/RewardSelector.test.tsx

### Implementation for User Story 4

- [X] T044 [US4] Implement child available rewards query (family + per-child + default fallback) in backend/src/main/java/com/example/readingrewards/domain/service/reward/ChildRewardAvailabilityService.java
- [X] T045 [US4] Implement select/change reward endpoints with locked snapshot logic in backend/src/main/java/com/example/readingrewards/domain/controller/ChildRewardSelectionController.java
- [X] T046 [US4] Enforce archived reward exclusion for new selections while preserving active in-progress selections in backend/src/main/java/com/example/readingrewards/domain/service/reward/RewardSelectionPolicyService.java
- [X] T047 [US4] Build child reward selector component with descriptive cards in frontend/src/components/rewards/RewardSelector.tsx
- [X] T048 [US4] Integrate reward selection prompt into child add-book flow in frontend/src/components/ReadingList.tsx

**Checkpoint**: Child can choose and update reward targets safely with predictable locked-rate calculations.

---

## Phase 7: User Story 5 - Parent and Child Track Reward Accumulation & Payouts (Priority: P1)

**Goal**: Both roles can see earned/paid/available balances and parent can confirm payouts.

**Independent Test**: Book completion updates ledger totals; parent payout confirmation updates balances and history.

### Tests for User Story 5

- [X] T049 [P] [US5] Add backend integration test for accumulation summary and payout confirmation in backend/src/test/java/com/example/readingrewards/integration/reward/RewardAccumulationIntegrationTest.java
- [X] T050 [P] [US5] Add frontend test for child balance widgets and payout state transitions in frontend/src/components/rewards/__tests__/RewardBalance.test.tsx

### Implementation for User Story 5

- [X] T051 [US5] Implement immutable accumulation ledger writes on book completion in backend/src/main/java/com/example/readingrewards/domain/service/reward/RewardAccumulationService.java
- [X] T052 [US5] Implement parent child-accumulation and payout-confirm endpoints in backend/src/main/java/com/example/readingrewards/domain/controller/ParentPayoutController.java
- [X] T053 [US5] Implement child balance and history endpoints in backend/src/main/java/com/example/readingrewards/domain/controller/ChildRewardBalanceController.java
- [X] T054 [US5] Build child rewards balance/history UI with by-type formatting in frontend/src/components/rewards/RewardBalance.tsx
- [X] T055 [US5] Build parent pending payouts and confirm action panel in frontend/src/components/parent/ParentPayoutsPanel.tsx
- [X] T056 [US5] Integrate balance/history/confirm APIs in frontend/src/services/rewardApi.ts

**Checkpoint**: Reward earnings and payouts are transparent and auditable for both parent and child.

---

## Phase 8: User Story 9 - No Rewards Shown for Parent Accounts (Priority: P1)

**Goal**: Parent sees only management experience; child sees personal rewards experience.

**Independent Test**: Parent navigation never exposes personal balance page while child retains full personal rewards access.

### Tests for User Story 9

- [X] T057 [P] [US9] Add frontend routing test for role-specific rewards navigation labels in frontend/src/components/__tests__/RewardsNavigationRoleGate.test.tsx
- [X] T058 [P] [US9] Add backend authorization test preventing parent access to child reward endpoints in backend/src/test/java/com/example/readingrewards/security/RewardRoleAuthorizationTest.java

### Implementation for User Story 9

- [X] T059 [US9] Implement role-aware rewards nav labels and route destinations in frontend/src/App.tsx
- [X] T060 [US9] Build parent manage-rewards landing page shell without personal balance widgets in frontend/src/pages/ParentRewards/ManageFamilyRewardsPage.tsx
- [X] T061 [US9] Enforce child-only access policy on /api/child/rewards endpoints in backend/src/main/java/com/example/readingrewards/security/RewardAccessPolicy.java

**Checkpoint**: Role separation for rewards UI and API is enforced end-to-end.

---

## Phase 9: User Story 6 - Progress Tracking: Chapters vs Pages (Priority: P2)

**Goal**: Support chapter/page tracking rules with OpenLibrary page-count fallback.

**Independent Test**: Per-chapter rewards require chapter data; per-book rewards permit chapters/pages/none, with page-count suggestions and manual override.

### Tests for User Story 6

- [X] T062 [P] [US6] Add backend test for mandatory chapter tracking validation in backend/src/test/java/com/example/readingrewards/unit/reward/ProgressTrackingValidationTest.java
- [X] T063 [P] [US6] Add backend integration test for OpenLibrary fallback and manual page entry in backend/src/test/java/com/example/readingrewards/integration/reward/OpenLibraryProgressIntegrationTest.java
- [X] T064 [P] [US6] Add frontend test for chapter/page tracking controls and completion gating in frontend/src/components/rewards/__tests__/ProgressTracker.test.tsx

### Implementation for User Story 6

- [X] T065 [US6] Implement chapter and page progress update endpoints in backend/src/main/java/com/example/readingrewards/domain/controller/ChildProgressTrackingController.java
- [X] T066 [US6] Implement progress validation policy for per-chapter vs per-book rewards in backend/src/main/java/com/example/readingrewards/domain/service/reward/ProgressTrackingPolicyService.java
- [X] T067 [US6] Implement OpenLibrary page count fetch + manual override fallback service in backend/src/main/java/com/example/readingrewards/external/OpenLibraryClient.java
- [X] T068 [US6] Build child progress tracking UI (chapters/pages/none) in frontend/src/components/rewards/ProgressTracker.tsx
- [X] T069 [US6] Integrate progress tracking API into reading detail flow in frontend/src/components/ReadingList.tsx

**Checkpoint**: Progress tracking is flexible while enforcing per-chapter completion correctness.

---

## Phase 10: User Story 7 - Kids Nudge Parents for Pending Payouts (Priority: P2)

**Goal**: Child can send payout reminders; parent receives in-app and optional email notifications.

**Independent Test**: Child sends reminder, parent receives notification, and reminder appears in payout action history.

### Tests for User Story 7

- [X] T070 [P] [US7] Add backend integration test for payout reminder message flow and status transition in backend/src/test/java/com/example/readingrewards/integration/message/PayoutReminderFlowIntegrationTest.java
- [X] T071 [P] [US7] Add backend unit test for parent email opt-in default and opt-out behavior in backend/src/test/java/com/example/readingrewards/unit/message/PayoutReminderEmailPreferenceTest.java
- [X] T072 [P] [US7] Add frontend test for child notify-parent interaction and confirmation state in frontend/src/components/rewards/__tests__/PayoutReminderAction.test.tsx

### Implementation for User Story 7

- [X] T073 [US7] Implement child payout reminder endpoint and message creation in backend/src/main/java/com/example/readingrewards/domain/controller/ChildMessageController.java
- [X] T074 [US7] Implement parent in-app notification retrieval for payout reminders in backend/src/main/java/com/example/readingrewards/domain/controller/ParentMessageController.java
- [X] T075 [US7] Implement optional parent email notification policy with default-enabled setting in backend/src/main/java/com/example/readingrewards/domain/service/message/PayoutReminderNotificationService.java
- [X] T076 [US7] Build child pending payout reminder action component in frontend/src/components/rewards/PayoutReminder.tsx
- [X] T077 [US7] Build parent reminder inbox panel in rewards management page in frontend/src/components/parent/PayoutReminderInbox.tsx

**Checkpoint**: Payout reminder loop is operational with in-app delivery and optional parent email support.

---

## Phase 11: User Story 8 - Parents Send Encouragement Messages (Priority: P2)

**Goal**: Parent sends in-app encouragement messages to child (no child email).

**Independent Test**: Parent sends encouragement; child receives persistent in-app message history.

### Tests for User Story 8

- [X] T078 [P] [US8] Add backend integration test for encouragement message send/read workflow in backend/src/test/java/com/example/readingrewards/integration/message/EncouragementMessageIntegrationTest.java
- [X] T079 [P] [US8] Add frontend test for parent send-message form and child inbox rendering in frontend/src/components/rewards/__tests__/EncouragementMessaging.test.tsx

### Implementation for User Story 8

- [X] T080 [US8] Implement parent encouragement send endpoint with in-app-only policy in backend/src/main/java/com/example/readingrewards/domain/controller/ParentMessageController.java
- [X] T081 [US8] Implement child message inbox and read-state endpoint in backend/src/main/java/com/example/readingrewards/domain/controller/ChildMessageController.java
- [X] T082 [US8] Build parent encouragement composer component in frontend/src/components/parent/EncouragementComposer.tsx
- [X] T083 [US8] Build child message center component with timestamped history in frontend/src/components/rewards/MessageCenter.tsx

**Checkpoint**: Parent-to-child encouragement messaging is fully functional and persists in-app history.

---

## Phase 12: User Story 10 - Rewards Page Handles All Reward Types (Priority: P2)

**Goal**: Rewards UI and calculations correctly represent money, time, and custom reward types.

**Independent Test**: Child with mixed reward types sees accurate, type-appropriate totals and labels on one consolidated page.

### Tests for User Story 10

- [X] T084 [P] [US10] Add backend unit test for money/time/custom formatting and aggregation rules in backend/src/test/java/com/example/readingrewards/unit/reward/RewardTypeFormattingServiceTest.java
- [X] T085 [P] [US10] Add frontend visual and behavior test for multi-type rewards rendering in frontend/src/components/rewards/__tests__/RewardsTypeDisplay.test.tsx

### Implementation for User Story 10

- [X] T086 [US10] Implement reward-type formatting and aggregation service in backend/src/main/java/com/example/readingrewards/domain/service/reward/RewardTypeFormattingService.java
- [X] T087 [US10] Implement by-reward-type summary in child balance response DTO mapping in backend/src/main/java/com/example/readingrewards/domain/dto/reward/ChildRewardBalanceDto.java
- [X] T088 [US10] Build consolidated reward type cards with distinct visual styles in frontend/src/components/rewards/RewardsTypeCards.tsx
- [X] T089 [US10] Integrate multi-type cards into child rewards page in frontend/src/pages/ChildRewards/ChildRewardsPage.tsx

**Checkpoint**: Consolidated rewards page handles all supported reward types with clear and accurate presentation.

---

## Phase 13: Polish & Cross-Cutting Concerns

**Purpose**: Final hardening, documentation, accessibility/performance validation, and release readiness.

- [ ] T090 [P] Add end-to-end regression flow (parent config -> child select -> earn -> remind -> payout) in tests/e2e/reward-customization.spec.ts
- [ ] T091 [P] Add accessibility assertions for rewards pages in tests/e2e/reward-accessibility.spec.ts
- [ ] T092 [P] Add frontend Lighthouse/perf baseline script for rewards page load target in scripts/phase8-rewards-performance-audit.mjs
- [ ] T093 Update quickstart validation steps and screenshots for new flows in specs/004-reward-customization/quickstart.md
- [ ] T094 Update feature research with final edge-case implementation notes in specs/004-reward-customization/research.md
- [ ] T095 Add release notes and rollout checklist for reward customization in docs/reward-customization-release.md

**Checkpoint**: Feature is validated for correctness, accessibility, performance, and rollout readiness.

---

## Dependencies & Execution Order

### Phase Dependencies

- Phase 1 (Setup): No dependencies.
- Phase 2 (Foundational): Depends on Phase 1; blocks all story phases.
- Phase 3-12 (User Stories): Depend on Phase 2 completion.
- Phase 13 (Polish): Depends on completion of all targeted user stories.

### User Story Dependency Graph

- US1 -> US2 -> US4 -> US5 -> US7
- US3 -> US1 (canned templates feed global defaults UX)
- US9 -> (independent after Phase 2; should be finished before broad UAT)
- US6 -> US4 and US5 (progress tracking extends reward selection/accumulation)
- US8 -> US7 (shares messaging infrastructure, no payout coupling)
- US10 -> US5 (requires accumulation and balance baseline)

### Recommended Dependency-Ordered Implementation Sequence

1. US1 (global defaults)
2. US3 (canned templates)
3. US2 (per-child overrides)
4. US4 (child selection)
5. US5 (accumulation and payouts)
6. US9 (parent/child UI separation hardening)
7. US6 (chapter/page progress)
8. US7 (payout reminders)
9. US8 (encouragement messages)
10. US10 (multi-type rewards presentation)

---

## Parallel Execution Opportunities

### Setup & Foundation

- Run T003, T004, T005, T006, T007 in parallel after T001-T002 are created.
- Run T009-T014 and T018-T020 in parallel after T008 starts.

### User Story Parallel Batches

- US1: T021 and T022 in parallel, then T026 and T027 in parallel after T023-T025.
- US2: T029 and T030 in parallel, then T033 and T034 in parallel after T031-T032.
- US3: T036 and T037 in parallel, then T040 and T041 in parallel after T038-T039.
- US4: T042 and T043 in parallel, then T047 and T048 in parallel after T044-T046.
- US5: T049 and T050 in parallel, then T054 and T055 in parallel after T051-T053.
- US6: T062, T063, T064 in parallel, then T068 and T069 in parallel after T065-T067.
- US7: T070, T071, T072 in parallel, then T076 and T077 in parallel after T073-T075.
- US8: T078 and T079 in parallel, then T082 and T083 in parallel after T080-T081.
- US10: T084 and T085 in parallel, then T088 and T089 in parallel after T086-T087.

---

## Acceptance & Completion Checkpoints

- **MVP Checkpoint (after Phase 7)**: US1 + US2 + US3 + US4 + US5 complete with passing contract/integration/component tests.
- **Role Safety Checkpoint (after Phase 8)**: US9 role gating verified in routing and API authorization tests.
- **P2 Capability Checkpoint (after Phase 12)**: US6 + US7 + US8 + US10 complete with mixed reward and messaging flows validated.
- **Release Checkpoint (after Phase 13)**: E2E, accessibility, performance, quickstart, and rollout docs complete.

---

## Implementation Strategy

### MVP First

1. Finish Phase 1-2.
2. Deliver US1, US3, US2, US4, US5.
3. Run checkpoint tests and demo parent/child core loop.

### Incremental Delivery

1. Add US9 to lock role UX clarity.
2. Add US6 for tracking flexibility.
3. Add US7 and US8 for messaging loops.
4. Add US10 for full multi-type display polish.

### Team Parallelization

1. Team A: Backend domain/services/controllers per story.
2. Team B: Frontend pages/components/services per story.
3. Team C: Contract/integration/E2E + docs/checkpoints across phases.
