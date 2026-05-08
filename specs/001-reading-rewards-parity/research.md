# Research: Reading Rewards Parity Rebuild

## Decision 1: Preserve public API semantics while reorganizing backend internals

- **Decision**: Keep the legacy endpoint surface and auth semantics for the critical flows covered by the parity spec, while moving business logic into service-layer modules aligned to auth, books, progress, rewards, and parent management.
- **Rationale**: This keeps parity verification concrete and limits the risk that UI behavior changes accidentally during the rewrite.
- **Alternatives considered**:
  - Keep controller-centric logic structure from the legacy app. Rejected because it preserves technical debt instead of rebuilding for maintainability.
  - Redesign the API first. Rejected because the user explicitly requested behavior preservation.

## Decision 2: Use Java 21 LTS with Spring Boot 3.5.6 in the new repo

- **Decision**: Target Java 21 for the backend runtime and use Spring Boot 3.5.6.
- **Rationale**: Java 21 provides stable LTS compatibility across local and container builds and aligns with the current backend Docker image/runtime.
- **Alternatives considered**:
  - Stay on Java 17. Rejected because Java 21 is the current LTS target and already validated in this repository.
  - Use Java 25. Rejected because it caused avoidable container compatibility issues for this implementation.

## Decision 3: Replace MUI with plain React and CSS

- **Decision**: Rebuild the frontend using React, React Router, typed API helpers, and hand-authored CSS rather than a component library.
- **Rationale**: The user explicitly deprioritized styling parity and requested easier maintenance. Removing component-library abstraction reduces upgrade and styling complexity.
- **Alternatives considered**:
  - Keep MUI for faster parity. Rejected because it adds styling and dependency overhead the user no longer wants.

## Decision 4: Prioritize backend and frontend automated coverage first

- **Decision**: Implement and run backend and frontend automated tests as required parity gates, and track Playwright smoke tests as follow-on work.
- **Rationale**: Backend and frontend tests are currently in place and actively used for regression control. E2E coverage remains planned but is not yet part of the committed test suite.
- **Alternatives considered**:
  - Blocking completion on immediate Playwright coverage. Rejected for now to avoid delaying stabilization of core parity flows.

## Decision 5: Keep secret handling external from day one

- **Decision**: The new repo will use env templates, startup validation, and local env files that remain ignored by git.
- **Rationale**: The legacy app currently exposes secrets in tracked files, which must not be repeated in the successor repo.
- **Alternatives considered**:
  - Copy the legacy `.env.sh` pattern initially and clean it later. Rejected because it would recreate the exact problem being fixed.