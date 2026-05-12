# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]
**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

**Language/Version**: Java 21, TypeScript 5.x  
**Primary Dependencies**: Spring Boot 3.5.6 (backend), React 18+ with Vite (frontend), Radix UI  
**Storage**: PostgreSQL with Flyway migrations  
**Testing**: JUnit 5, Mockito (backend); Vitest, Playwright (frontend)  
**Target Platform**: Cloud-deployed (Render), Docker containerized  
**Project Type**: Full-stack web application (SPA + REST API)  
**Performance Goals**: Rewards page load <1s on 4G throttle (SC-005)  
**Constraints**: WCAG 2.1 AA accessibility compliance (SC-006), accurate payout calculations (SC-003)  
**Scale/Scope**: Family reward system for 10s of families, 100s of children; 5-10 entities per child

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**I. Behavior Parity First**: Legacy app has hardcoded $1/chapter reward. New feature replaces this with configurable per-family and per-child rewards. Parents default new children to $1/chapter (FR-015) to maintain surface parity. Design preserves child reward selection flow and balance tracking.

**II. Spec Before Structure**: Feature spec is comprehensive and mandatory before code. This plan artifacts (research.md, data-model.md, contracts/) derive from spec, not ad-hoc implementation.

**III. Tests Are Delivery Criteria**: Backend unit tests on RewardTemplate, RewardSelection, RewardAccumulation entities; controller tests on reward CRUD and selection endpoints; frontend component tests for RewardSelector and RewardsBalance; E2E smoke tests for parent reward config + child selection + payout flow.

**IV. Clean Architecture Over Legacy Shape**: Reward domain services separate from BookRead concerns. RewardTemplate (immutable config), RewardSelection (child choice), RewardAccumulation (ledger) are distinct entities. Messaging infrastructure (in-app, optional email) kept separate from reward calculations.

**V. Secure Configuration By Default**: No hardcoded email thresholds or API keys. Email notification defaults (FR-012) configured via environment; OpenLibrary API endpoint externalized. Database schema migrations track schema changes, secrets stored in deployment config only.

**Status**: ✅ Provisional pass - no parity violations identified. Re-check after Phase 1 design.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/example/readingrewards/
│   ├── domain/
│   │   ├── model/          # RewardTemplate, RewardSelection, RewardAccumulation, ProgressTracking, Message entities
│   │   ├── dto/            # RewardConfigDto, RewardSelectionDto, AccumulationDto, MessageDto
│   │   ├── service/        # RewardConfigService, RewardCalculationService, MessageService
│   │   ├── repo/           # RewardTemplateRepo, RewardSelectionRepo, AccumulationRepo, ProgressTrackingRepo, MessageRepo
│   │   └── controller/     # RewardController (parent: config), RewardSelectionController (child: select)
│   └── external/
│       └── OpenLibraryClient.java
└── test/
    ├── unit/               # Service & entity tests
    ├── integration/        # Controller tests with MockMvc
    └── fixtures/           # Test data builders

frontend/
├── src/
│   ├── components/
│   │   ├── rewards/        # RewardSelector, RewardBalance, RewardHistory, PayoutReminder, MessageCenter
│   │   └── parent/         # RewardConfiguration, RewardTemplateBuilder, PerChildOverrides
│   ├── pages/
│   │   ├── ParentRewards/  # Manage page (global + per-child config)
│   │   └── ChildRewards/   # Personal page (selection + balance)
│   └── services/           # rewardApi.ts, messageApi.ts, openlibrary.ts
└── test/
    ├── components/         # Vitest component tests
    └── e2e/               # Playwright smoke tests (parent config → child select → payout)
```

**Structure Decision**: Full-stack web application leveraging existing Spring Boot + React/TypeScript architecture. Reward feature adds new domain entities, repositories, and services on backend; new pages and components on frontend. OpenLibrary API client is new external integration. Message infrastructure is cross-cutting concern for in-app/email notifications.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
