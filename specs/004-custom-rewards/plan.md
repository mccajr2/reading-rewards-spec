# Implementation Plan: Parent Reward Customization

**Branch**: `[004-parent-reward-customization]` | **Date**: 2026-05-12 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-custom-rewards/spec.md`

## Summary

Introduce configurable rewards so parents can define multiple reward options at family and child scope, children can select active options, and earnings/payouts are tracked with unit-safe ledgers. Support per-chapter, per-book, and per-page milestone earning with carry-forward leftovers, include parent-approved payout/spend requests, and add constrained in-app messaging (child nudges with parent email notification, parent encouragement in-app only).

## Technical Context

**Language/Version**: Java 21 + Spring Boot 3.5.6 (backend), TypeScript 5.9 + React 19 + Vite 7 (frontend)  
**Primary Dependencies**: Spring Web/Data JPA/Security/Validation, Flyway, PostgreSQL driver; React Router 7, Vitest, Testing Library  
**Storage**: PostgreSQL (primary), H2 for local/test profiles  
**Testing**: JUnit/Spring Boot Test + Spring Security Test (backend), Vitest + Testing Library (frontend), Playwright E2E smoke  
**Target Platform**: Web application (browser frontend + Linux containerized backend)  
**Project Type**: Monorepo web app (`backend/` + `frontend/`)  
**Performance Goals**: Preserve existing user-perceived responsiveness; reward calculation and balance retrieval remain interactive for normal family usage (<2s API responses in standard flows)  
**Constraints**: Preserve auth/role boundaries; no parent reward earning UI; one nudge per child per 24h; separate ledgers per reward unit; no secrets in repo  
**Scale/Scope**: Family-level and child-level rewards across existing user base; feature touches rewards, reading progress, ledger, and messaging surfaces

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Design Gate Review

✅ **I. Behavior Parity First**  
- Feature intentionally extends rewards behavior through approved spec updates.
- Existing auth and role constraints are preserved (parents configure, children earn/select).

✅ **II. Spec Before Structure**  
- Spec exists at `specs/004-custom-rewards/spec.md` with clarified decisions and measurable outcomes.

✅ **III. Tests Are Delivery Criteria**  
- Plan includes backend calculation/authorization tests, frontend flow tests, and E2E smoke for critical journey.

✅ **IV. Clean Architecture Over Legacy Shape**  
- Uses explicit reward option, selection, ledger, and settlement models instead of controller-only logic sprawl.

✅ **V. Secure Configuration By Default**  
- No new secret classes introduced; notification and metadata integrations use existing env/config patterns.

**Pre-Design Result**: PASS

### Post-Design Gate Review (after Phase 1 artifacts)

✅ Research, data model, contracts, and quickstart now define deterministic behavior for all clarified requirements.  
✅ No unresolved clarification markers remain.  
✅ No constitution violations introduced.

**Post-Design Result**: PASS

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
└── tasks.md              # Created by /speckit.tasks
```

### Source Code (repository root)

```text
backend/
├── src/main/java/.../domain/           # reward options, ledgers, settlement, messaging entities
├── src/main/java/.../service/          # scope resolution, earnings, settlement approval
├── src/main/java/.../web/              # reward config, selection, settlement, messaging APIs
├── src/main/resources/db/migration/    # schema migrations for new reward tables/fields
└── src/test/java/.../                  # service/controller tests for calculations and permissions

frontend/
├── src/components/                     # parent reward config, child selection, ledger views, messaging UI
├── src/services/                       # API clients for reward options, ledgers, settlement, messages
├── src/types/                          # DTOs for reward customization entities
└── src/tests/                          # component and integration tests

tests/e2e/
└── reward-customization*.spec.*        # end-to-end smoke flows
```

**Structure Decision**: Keep existing monorepo boundaries and implement feature incrementally across current backend/frontend modules. No new top-level services are required.

## Phase 0: Research Output

Phase 0 completed in `research.md` with explicit decisions for:
- additive scope resolution,
- unit-separated ledgers,
- page milestone carry-forward,
- child-request/parent-approve settlement,
- nudge cooldown and channel rules,
- OpenLibrary page suggestion with override.

## Phase 1: Design and Contracts Output

Phase 1 completed with:
- `data-model.md` defining entities, relationships, and state transitions,
- API and behavior contracts under `contracts/`,
- implementation and verification workflow in `quickstart.md`.

## Phase 2 Preview (for /speckit.tasks)

Planned task streams:
1. Backend schema and domain model updates.
2. Earnings engine and ledger logic updates.
3. Settlement request + approval endpoints.
4. Messaging constraints and notifications.
5. Frontend parent/child reward UX and tracking updates.
6. Automated tests and E2E smoke coverage.

## Complexity Tracking

No constitution violations requiring justification.
