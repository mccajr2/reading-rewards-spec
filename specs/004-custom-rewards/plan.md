# Implementation Plan: Parent Reward Customization Re-Baseline

**Branch**: `004-parent-reward-customization` | **Date**: 2026-05-12 | **Spec**: `/specs/004-custom-rewards/spec.md`
**Input**: Feature specification from `/specs/004-custom-rewards/spec.md`

## Summary

Re-baseline feature 004 so implemented behavior matches clarified requirements: separate parent reward management from kid management, support typed monetary and non-monetary reward values, require per-book earning basis selection at book start, lock that basis for the in-progress book, require explicit completion-time option choice when multiple options match, require PER_PAGE page-count confirmation or override before earning, and treat PER_BOOK chapter/page counts as optional tracking-only data.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.9 and React 19 (frontend)  
**Primary Dependencies**: Spring Boot 3.5.6, Spring Security, Spring Data JPA, Flyway, PostgreSQL, Vite 7, React Router 7, Vitest, Testing Library  
**Storage**: PostgreSQL primary, H2 fallback for local/testing profiles  
**Testing**: JUnit and Spring Boot Test, Spring Security Test, Vitest and Testing Library, Playwright smoke coverage  
**Target Platform**: Web application on browser frontend plus JVM backend (local macOS dev, Linux deploy target)  
**Project Type**: Monorepo web application with backend and frontend modules  
**Performance Goals**: Preserve current UX responsiveness; no significant regression in existing reward and reading flows  
**Constraints**: Behavior parity governance, no live secrets in tracked files, parent and child role boundaries unchanged, explicit validation for typed value model and confirmation gates  
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
