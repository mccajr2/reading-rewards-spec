# Implementation Plan: Reading Rewards Parity Rebuild

**Branch**: `001-reading-rewards-parity` | **Date**: 2026-05-07 | **Spec**: `/specs/001-reading-rewards-parity/spec.md`
**Input**: Feature specification from `/specs/001-reading-rewards-parity/spec.md`

## Summary

Create a new standalone implementation of Reading Rewards in this repository by preserving the legacy app's observable behavior while replacing its internal structure with a cleaner backend and frontend split. The new backend will expose stable contracts for authentication, reading progress, and rewards. The new frontend will keep the same key workflows with plain React and CSS. The project will be validated with backend, frontend, and end-to-end tests from the start.

## Technical Context

**Language/Version**: Java 21, TypeScript 5.9, Node.js 20+  
**Primary Dependencies**: Spring Boot 3.5.6, Spring Security, Spring Data JPA, Flyway, PostgreSQL driver, React 19, React Router, Vite, Vitest, React Testing Library  
**Storage**: PostgreSQL  
**Testing**: JUnit 5, Spring Boot Test, Vitest, React Testing Library  
**Target Platform**: macOS and Linux developer environments, container-friendly web deployment  
**Project Type**: Full-stack web application  
**Performance Goals** *(local developer-experience guidance targets, not contractual SLOs)*: Local startup under 30 seconds, interactive page transitions under 200 ms on normal local data volumes, API response times under 300 ms p95 for core CRUD flows in local validation  
**Constraints**: Preserve legacy API semantics for approved flows, no live secrets in tracked files, keep the original repository untouched, plain CSS over component-library lock-in  
**Scale/Scope**: Single family-oriented web app with parent and child roles, one backend service, one SPA frontend, and one PostgreSQL database

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Behavior parity remains the primary constraint for all external routes and flows.
- Spec-first workflow is satisfied by the existence of the current spec, this plan, and follow-on task artifacts.
- Test coverage is planned at backend, frontend, and end-to-end levels for all P1 journeys.
- The target structure explicitly avoids reproducing controller-heavy legacy coupling.
- Secret hygiene is enforced through `.env.example`, startup validation, and removal of live secrets from tracked files.

## Project Structure

### Documentation (this feature)

```text
specs/001-reading-rewards-parity/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
```

### Source Code (repository root)

```text
backend/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/example/readingrewards/
│   │   │   ├── auth/
│   │   │   ├── domain/
│   │   │   ├── shared/
│   │   │   └── ReadingRewardsApplication.java
│   │   └── resources/
│   └── test/
│       └── java/com/example/readingrewards/
└── Dockerfile

frontend/
├── package.json
├── vite.config.ts
├── src/
│   ├── app/
│   ├── features/
│   │   ├── auth/
│   │   ├── books/
│   │   ├── nav/
│   │   ├── parent/
│   │   └── rewards/
│   ├── shared/
│   └── styles/
└── nginx.conf
```

**Structure Decision**: Use a two-application repository with a bounded-context backend layout and a feature-first frontend layout. This keeps the rebuilt app maintainable while preserving the legacy behavior at the API and user-flow boundaries.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Dual application repo | Required to preserve separate backend and SPA deployment shape | A single-process app would change deployment and UI integration assumptions |
| Compatibility layer around legacy API semantics | Needed to preserve route and payload behavior while cleaning internals | Rewriting API contracts would break existing user-facing behavior and parity validation |