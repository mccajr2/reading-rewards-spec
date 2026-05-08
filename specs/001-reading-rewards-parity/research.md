# Research: Reading Rewards Parity Rebuild

## Decision 1: Preserve public API semantics while reorganizing backend internals

- **Decision**: Keep the legacy endpoint surface and auth semantics for the critical flows covered by the parity spec, while moving business logic into service-layer modules aligned to auth, books, progress, rewards, and parent management.
- **Rationale**: This keeps parity verification concrete and limits the risk that UI behavior changes accidentally during the rewrite.
- **Alternatives considered**:
  - Keep controller-centric logic structure from the legacy app. Rejected because it preserves technical debt instead of rebuilding for maintainability.
  - Redesign the API first. Rejected because the user explicitly requested behavior preservation.

## Decision 2: Use Java 25 with a current Spring Boot line in the new repo

- **Decision**: Target Java 25 for the new backend and pair it with a current Spring Boot release that supports that runtime.
- **Rationale**: The new repository is intended to be the modern successor, so a fresh runtime target is appropriate. Toolchain risk is acceptable because the new repo is isolated from the production baseline.
- **Alternatives considered**:
  - Stay on Java 21 because it is already installed in the legacy environment. Rejected because the new repo is explicitly intended to move forward on versions.
  - Use an unreleased or preview Java line. Rejected because the project goal is maintainability, not experimentation.

## Decision 3: Replace MUI with plain React and CSS

- **Decision**: Rebuild the frontend using React, React Router, typed API helpers, and hand-authored CSS rather than a component library.
- **Rationale**: The user explicitly deprioritized styling parity and requested easier maintenance. Removing component-library abstraction reduces upgrade and styling complexity.
- **Alternatives considered**:
  - Keep MUI for faster parity. Rejected because it adds styling and dependency overhead the user no longer wants.

## Decision 4: Treat end-to-end coverage as part of parity validation

- **Decision**: Use Playwright smoke tests for the parent and child critical journeys in addition to backend and frontend tests.
- **Rationale**: The rewrite crosses both layers, so contract-level and component-level tests alone are not enough to prove parity.
- **Alternatives considered**:
  - Backend and frontend tests only. Rejected because it leaves cross-layer workflow risk unverified.

## Decision 5: Keep secret handling external from day one

- **Decision**: The new repo will use env templates, startup validation, and local env files that remain ignored by git.
- **Rationale**: The legacy app currently exposes secrets in tracked files, which must not be repeated in the successor repo.
- **Alternatives considered**:
  - Copy the legacy `.env.sh` pattern initially and clean it later. Rejected because it would recreate the exact problem being fixed.