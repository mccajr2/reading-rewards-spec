# Reading Rewards Spec Constitution

## Core Principles

### I. Behavior Parity First
The new repository MUST preserve the legacy app's externally visible behavior unless a deviation is explicitly documented in the active feature spec. Endpoint paths, auth gating, parent and child permissions, reading progression semantics, and reward calculations are treated as contracts until replaced through an approved spec change.

### II. Spec Before Structure
Every significant change MUST begin as a spec artifact before implementation proceeds. Architecture, schemas, and tests are derived from approved specs rather than inferred ad hoc during coding.

### III. Tests Are Delivery Criteria
No feature is complete without automated verification. Backend unit and controller tests, frontend component tests, and end-to-end smoke coverage for critical journeys are mandatory for parity features.

### IV. Clean Architecture Over Legacy Shape
The successor implementation MUST favor maintainable module boundaries, explicit domain logic, and narrow interfaces. Reproducing controller-level technical debt from the legacy app is forbidden unless it is the smallest safe way to preserve a required behavior.

### V. Secure Configuration By Default
Tracked files MUST never contain live secrets. All environments use template-driven configuration, validated startup settings, and explicit deployment documentation. Secret rotation and sanitization are part of the migration, not optional cleanup.

## Technical Constraints

The project is a web application split into backend and frontend modules inside a single repository. The backend uses a current practical stable Java and Spring Boot stack with PostgreSQL and Flyway. The frontend uses a current practical stable React, TypeScript, and Vite stack with plain CSS favored over component-library styling. Tooling choices MUST prefer long-term maintainability and stable ecosystem support over novelty.

## Workflow And Quality Gates

Each feature MUST include a spec, implementation plan, and task list. The constitution check in each plan must verify behavior parity, test coverage, secret hygiene, and deployment readiness. Before code is considered complete, the repository must pass backend tests, frontend tests, and end-to-end smoke checks for the affected journeys.

## Governance

This constitution governs all work in this repository and overrides convenience-driven shortcuts. Any amendment requires a documented rationale, an update to affected specs or plans, and a migration note if the change alters developer workflow or verification expectations.

**Version**: 1.0.0 | **Ratified**: 2026-05-07 | **Last Amended**: 2026-05-07
