
# Implementation Plan: Parent Reward Customization (Clarified Requirements)

**Branch**: `004-parent-reward-customization` | **Date**: 2026-05-12 | **Spec**: `/specs/004-custom-rewards/spec.md`
**Input**: Feature specification from `/specs/004-custom-rewards/spec.md`

## Summary

This plan implements clarified requirements for parent reward customization:

- **Reward name/description is inferred, not manually entered.**
- **Default $1/chapter reward is non-editable but can be deactivated.**
- **At least one reward must always be active.**
- **Reward selection/change is available in the reading/progress UI, with prompts when multiple rewards are eligible.**
- **For PER_BOOK rewards, selection occurs at book completion via a 'Mark as Complete' action.**

All changes affect backend, frontend, and test coverage. See below for details.


## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.9 and React 19 (frontend)  
**Primary Dependencies**: Spring Boot 3.5.6, Spring Security, Spring Data JPA, Flyway, PostgreSQL, Vite 7, React Router 7, Vitest, Testing Library  
**Storage**: PostgreSQL primary, H2 fallback for local/testing profiles  
**Testing**: JUnit and Spring Boot Test, Spring Security Test, Vitest and Testing Library, Playwright smoke coverage  
**Target Platform**: Web application on browser frontend plus JVM backend (local macOS dev, Linux deploy target)  
**Project Type**: Monorepo web application with backend and frontend modules  
**Performance Goals**: Preserve current UX responsiveness; no significant regression in existing reward and reading flows  
**Constraints**: Behavior parity governance, no live secrets in tracked files, parent and child role boundaries unchanged, explicit validation for typed value model and confirmation gates, reward name/description inference, non-editable default, at-least-one-active enforcement  
**Scale/Scope**: Existing family and child workflows; feature-local schema, API, and UI changes only

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Behavior Parity First**: PASS. All intentional behavior changes are documented in spec clarifications and FR-008a..FR-024.
- **Spec Before Structure**: PASS. This plan and design outputs are derived from the clarified feature spec.
- **Tests Are Delivery Criteria**: PASS. Plan mandates backend, frontend, and smoke test updates for new gating behavior.
- **Clean Architecture Over Legacy Shape**: PASS. Design keeps explicit domain entities and clear precedence rules.
- **Secure Configuration By Default**: PASS. No new secret-handling or configuration policy exceptions.


## Project Structure

### Documentation (this feature)

```text
specs/004-custom-rewards/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── reward-customization.openapi.yaml
│   └── earnings-and-ledger-rules.md
└── tasks.md
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/example/readingrewards/
│   ├── auth/
│   └── domain/
│       ├── controller/
│       ├── dto/
│       ├── model/
│       ├── repo/
│       └── service/
├── src/main/resources/db/migration/
└── src/test/java/com/example/readingrewards/

frontend/
├── src/
│   ├── app/
│   ├── components/
│   ├── features/
│   │   ├── parent/
│   │   └── rewards/
│   ├── shared/
│   └── test/
└── package.json
```

**Structure Decision**: Keep the current monorepo architecture and implement clarified behavior inside existing domain and feature boundaries.


## Phase 0: Research Output

Generated: `/specs/004-custom-rewards/research.md`

Clarified decisions include additive visibility, typed value model, fallback-selection precedence, per-book basis lock, explicit completion-time choice blocking, PER_PAGE page-count confirmation gate, and PER_BOOK tracking optionality.


## Phase 1: Design Output

Generated:
- `/specs/004-custom-rewards/data-model.md`
- `/specs/004-custom-rewards/contracts/reward-customization.openapi.yaml`
- `/specs/004-custom-rewards/contracts/earnings-and-ledger-rules.md`
- `/specs/004-custom-rewards/quickstart.md`

Agent context update:
- `.github/copilot-instructions.md` already points to `specs/004-custom-rewards/plan.md`; no marker update required.


## Post-Design Constitution Re-Check

- **Behavior Parity First**: PASS
- **Spec Before Structure**: PASS
- **Tests Are Delivery Criteria**: PASS
- **Clean Architecture Over Legacy Shape**: PASS
- **Secure Configuration By Default**: PASS

## Complexity Tracking

No constitution exceptions required.

---

## Implementation Details

### Backend

- **Reward name/description inference**: Remove all manual entry fields for reward name/description from API/model. Name/description is always generated using a standardized template (e.g., "$X per Y pages/chapter/book").
- **Default reward**: On family creation, always create a default $1/chapter reward (non-editable, but can be deactivated). Prevent deletion or editing of this reward. Allow toggling `isActive`, but only if at least one other reward is (or will be) active—never allow zero active rewards for a child/family.
- **At-least-one-active enforcement**: Enforce at least one active reward per child/family in the service layer. Prevent deactivation if it would leave zero active rewards. Add validation and error reporting for this case.
- **Reward selection/change**: Expose endpoints to get eligible rewards for a child, set active reward, and handle selection at reading/progress events. For PER_BOOK, allow selection at 'Mark as Complete' only if the book was set up for PER_BOOK tracking. The user must specify per-book, per-chapter, or per-page tracking when adding a book. The 'Mark as Complete' button is only available for PER_BOOK tracking. For other types, selection occurs at progress event.
- **Eligibility prompts**: When multiple rewards are eligible for a reading event, require explicit selection in the API (block completion if not provided). Only show selection UI when multiple eligible rewards exist.
- **Tests**: Add/extend unit and integration tests for:
    - Default reward creation and non-editability
    - At-least-one-active enforcement (cannot deactivate last active reward)
    - Reward selection logic and eligibility prompts (selection UI only when multiple eligible)
    - PER_BOOK selection at completion (only for PER_BOOK tracking)

### Frontend

- **Reward name/description**: Remove manual entry fields from reward creation/edit UI. Display inferred name/description (e.g., "$1 per chapter", "1 TV show per book") using the standardized template.
- **Default reward**: Show default $1/chapter reward as non-editable in the UI. Allow toggling active/inactive, but only if another reward is active. Never allow all rewards to be deactivated. Show error if attempted.
- **At-least-one-active enforcement**: Disable deactivation UI if only one reward is active. Show error or inline message if attempted.
- **Reward selection/change**: In reading/progress UI, show eligible rewards and allow selection/change only when multiple eligible rewards exist. The selection/change UI is only shown as a prompt when logging progress, not as a persistent UI element. For PER_BOOK, show a 'Mark as Complete' button only if the book was set up for PER_BOOK tracking; this triggers reward selection if multiple eligible rewards exist.
- **Tests**: Add/extend component and integration tests for:
    - Default reward display and non-editability
    - At-least-one-active enforcement in UI (cannot deactivate last active reward)
    - Reward selection/change flows and eligibility prompts (UI only when multiple eligible)
    - PER_BOOK selection at completion (only for PER_BOOK tracking)

### End-to-End & Smoke Tests

- Add Playwright tests for:
    - Default reward presence and non-editability
    - At-least-one-active enforcement (cannot deactivate last active reward)
    - Reward selection/change in reading/progress UI (prompt only when multiple eligible)
    - PER_BOOK reward selection at completion (only for PER_BOOK tracking)

---

## Migration & Compatibility

- Migrate existing rewards to inferred name/description on deploy.
- Ensure all families have a default $1/chapter reward (active if none exist, inactive if at least one active reward exists).

---

## Open Questions / Risks

- How to handle legacy rewards with custom names? (Recommend: migrate to inferred format, store old name in audit log if needed.)
- UI/UX for at-least-one-active enforcement: error toast or inline message?

---

## Acceptance Criteria (Summary)

- Reward name/description is always inferred and not manually entered.
- Default $1/chapter reward is present, non-editable, and can only be deactivated if another reward is active (never allow zero active rewards).
- At least one reward is always active per child/family.
- Reward selection/change is available in reading/progress UI, with prompts only when multiple rewards are eligible, and only as a prompt (not persistent UI).
- For PER_BOOK rewards, selection occurs at book completion via a 'Mark as Complete' action, but only if the book was set up for PER_BOOK tracking.
