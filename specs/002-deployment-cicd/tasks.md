---
description: "Task list for Render Deployment with GitHub Actions CI/CD"
---

# Tasks: Render Deployment with GitHub Actions CI/CD

**Input**: Design documents from `/specs/002-deployment-cicd/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story. No test tasks are generated (none requested in spec).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create new source files required by the production deployment. These must exist before workflows can build or deploy.

- [X] T001 Create `backend/src/main/resources/application-prod.yml` — Spring `prod` profile that reads `${DATABASE_URL}`, `${DATABASE_USER}`, `${DATABASE_PASSWORD}`, `${FRONTEND_URL}` from env; sets `ddl-auto: validate`; enables `spring.flyway.enabled: true` with `locations: classpath:db/migration` (see plan §1.1 for exact YAML)
- [X] T002 Create `render.yaml` at repo root — adapt from `reading-rewards/render.yaml`: remove Render Postgres service, replace `dockerfilePath`/`autoDeploy:true` with `image.url: ghcr.io/[GITHUB_USERNAME]/reading-rewards-spec/backend:latest` and `image.url: ghcr.io/[GITHUB_USERNAME]/reading-rewards-spec/frontend:latest`, set `autoDeploy: false`, add `healthCheckPath: /actuator/health` for backend and `healthCheckPath: /` for frontend, add all env vars with `sync: false` for secrets (see plan §1.4 for complete YAML)

---

## Phase 2: Foundational (External Configuration — Blocking Prerequisites)

**Purpose**: One-time manual setup of secrets and Render services. Must be complete before any workflow can authenticate to GHCR, trigger Render deploys, or receive `VITE_API_URL` at build time.

**⚠️ CRITICAL**: GitHub Actions workflows will fail at the GHCR login, curl, or docker build-arg steps if these are not set up first.

- [X] T003 Add three GitHub repository secrets in GitHub → Settings → Secrets → Actions: `RENDER_BACKEND_DEPLOY_HOOK_URL` (Render Deploy Hook URL for backend service), `RENDER_FRONTEND_DEPLOY_HOOK_URL` (Render Deploy Hook URL for frontend service), `VITE_API_URL` (value: `https://reading-rewards-spec-backend.onrender.com/api`)
- [X] T004 [P] Create two Render web services in the Render dashboard using `render.yaml` as reference: `reading-rewards-spec-backend` (Docker, image from GHCR backend URL, free plan) and `reading-rewards-spec-frontend` (Docker, image from GHCR frontend URL, free plan) — copy Deploy Hook URLs for T003
- [X] T005 [P] Set Render environment variables for the backend service in Render dashboard: `SPRING_PROFILES_ACTIVE=prod`, `DATABASE_URL=jdbc:postgresql://...` (Neon JDBC URL with `jdbc:` prefix — see research.md §4), `DATABASE_USER`, `DATABASE_PASSWORD`, `FRONTEND_URL=https://reading-rewards-spec-frontend.onrender.com`

**Checkpoint**: GitHub Secrets configured, Render services created, Render env vars set — workflow execution can now proceed

---

## Phase 3: User Story 1 — Developer Opens Pull Request (Priority: P1) 🎯 MVP

**Goal**: GitHub Actions automatically runs backend and/or frontend tests on every PR to main, providing pass/fail feedback. Developers can see test output via the "Details" link on each check.

**Independent Test**: Create a feature branch, push a change under `backend/` or `frontend/`, open a PR to main, and verify the corresponding GitHub Actions workflow triggers and runs tests within 10 minutes (SC-002). Verify a PR with failing tests shows a failed check and does not allow merge.

### Implementation for User Story 1

- [X] T006 [US1] Create `.github/workflows/backend.yml` — `on` block: `push.branches: [main]` + `pull_request.branches: [main]` both with `paths: ['backend/**', '.github/workflows/backend.yml']`; single `test` job: `runs-on: ubuntu-latest`, checkout, `actions/setup-java@v4` with `java-version: '21'` and `distribution: temurin` and `cache: maven` and `cache-dependency-path: backend/pom.xml`, run `./mvnw test` with `working-directory: backend` (FR-001, FR-003, SC-001, SC-002)
- [X] T007 [P] [US1] Create `.github/workflows/frontend.yml` — `on` block: `push.branches: [main]` + `pull_request.branches: [main]` both with `paths: ['frontend/**', '.github/workflows/frontend.yml']`; single `test` job: `runs-on: ubuntu-latest`, checkout, `actions/setup-node@v4` with `node-version: '22'` and `cache: npm` and `cache-dependency-path: frontend/package-lock.json`, run `npm ci` then `npm test` with `working-directory: frontend` (FR-002, FR-004, SC-001, SC-002)

**Checkpoint**: Open a PR with backend or frontend changes — the matching workflow must appear in GitHub PR checks and report pass/fail within 10 minutes

---

## Phase 4: User Story 2 — Developer Merges PR to Main (Priority: P1)

**Goal**: Merging to main triggers automated Docker build → GHCR push → Render Deploy Hook for whichever service changed. Render pulls the new image, starts the service, runs health checks, and marks the deployment successful. VITE_API_URL is baked into the frontend image at build time.

**Independent Test**: Merge a PR to main; verify both workflows trigger the `build-and-push` job (if both services changed), images tagged `sha-[SHA]` and `latest` appear in GHCR Packages, Render deploy logs show a new deployment starting, and `/actuator/health` returns 200 within 30 seconds of backend service reaching running state (SC-003, SC-004, SC-006).

### Implementation for User Story 2

- [X] T008 [US2] Add `build-and-push` job to `.github/workflows/backend.yml` — `needs: test`, `if: github.ref == 'refs/heads/main' && github.event_name == 'push'`, `permissions: packages: write, contents: read`; steps: `docker/login-action@v3` with `registry: ghcr.io` and `username: ${{ github.actor }}` and `password: ${{ secrets.GITHUB_TOKEN }}`; `docker/metadata-action@v5` with `images: ghcr.io/${{ github.repository }}/backend` and `tags: type=sha,prefix=sha-` plus `type=raw,value=latest`; `docker/build-push-action@v5` with `context: ./backend`, `push: true`, tags from metadata action; `curl -fsS -X POST "${{ secrets.RENDER_BACKEND_DEPLOY_HOOK_URL }}"` (FR-005, FR-006, FR-007, FR-008, FR-015)
- [X] T009 [P] [US2] Add `build-and-push` job to `.github/workflows/frontend.yml` — same structure as T008 but `images: ghcr.io/${{ github.repository }}/frontend`, `context: ./frontend`, and add `build-args: VITE_API_URL=${{ secrets.VITE_API_URL }}` to the build-push action step; curl posts to `${{ secrets.RENDER_FRONTEND_DEPLOY_HOOK_URL }}` (FR-005, FR-006, FR-007, FR-008, FR-011, FR-015)
- [X] T010 [P] [US2] After first successful GHCR push (triggered by merging main): set both GHCR packages to Public visibility in GitHub → Packages → reading-rewards-spec/backend → Package settings → Change visibility → Public; repeat for `reading-rewards-spec/frontend` — required so Render can pull without auth (research.md §6, FR-009)

**Checkpoint**: Merge triggers `build-and-push`; GHCR shows new tagged images; Render deployment completes; `/actuator/health` and `/` return 200; frontend API calls reach backend via VITE_API_URL (SC-006, SC-007, SC-008)

---

## Phase 5: User Story 3 — Path-Triggered Workflows (Priority: P2)

**Goal**: Commits that only touch `frontend/**` do not trigger the backend workflow, and vice versa. Both workflows trigger in parallel when both services change.

**Independent Test**: Push a commit that modifies only `frontend/src/` — verify in GitHub Actions that only `frontend.yml` appears in the commit's workflow runs. Push a commit that modifies only `backend/src/` — verify only `backend.yml` triggers. Push a commit touching both — verify both run in parallel (SC-009).

### Implementation for User Story 3

> **Note**: Path triggers were included in T006 and T007 as part of the `on` block configuration. Verify correctness of path filters — no additional file changes needed unless the `on.paths` values require adjustment.

- [X] T011 [US3] Verify path filter correctness in `.github/workflows/backend.yml` — confirm `on.push.paths` and `on.pull_request.paths` both contain exactly `['backend/**', '.github/workflows/backend.yml']`; push a frontend-only commit to a test branch and open a PR to confirm backend workflow does NOT appear in PR checks (FR-003, SC-009)
- [X] T012 [P] [US3] Verify path filter correctness in `.github/workflows/frontend.yml` — confirm `on.push.paths` and `on.pull_request.paths` both contain exactly `['frontend/**', '.github/workflows/frontend.yml']`; push a backend-only commit to a test branch and confirm frontend workflow does NOT appear in PR checks (FR-004, SC-009)

**Checkpoint**: Path filtering is confirmed correct — backend and frontend workflows trigger independently based on which files changed

---

## Phase 6: User Story 4 — Clear Failure Reporting (Priority: P2)

**Goal**: When tests fail, developers see clear error output. When a Docker build or Render Deploy Hook call fails, the workflow step fails with a non-zero exit code and surfaces the error in the GitHub Actions log. The Render dashboard also shows the failure with deployment logs.

**Independent Test**: Break a backend test intentionally, push to a branch, open a PR — verify the backend workflow check shows a failure and the surefire report artifact is downloadable from the GitHub Actions run summary. Restore the test, merge, then intentionally pass a bad Deploy Hook URL via secret — verify the curl step fails the job with a clear error (SC-011, SC-012, FR-019).

### Implementation for User Story 4

- [X] T013 [US4] Add `if: failure()` upload-artifact step to the `test` job in `.github/workflows/backend.yml` — `uses: actions/upload-artifact@v4` with `name: surefire-reports` and `path: backend/target/surefire-reports/` — ensures test failure output is accessible in the workflow run summary (FR-019, SC-011)
- [X] T014 [P] [US4] Add `if: failure()` upload-artifact step to the `test` job in `.github/workflows/frontend.yml` — `uses: actions/upload-artifact@v4` with `name: frontend-test-results` and `path: frontend/test-results/` — ensures Vitest failure output is downloadable (FR-019, SC-011)
- [X] T015 [P] [US4] Confirm `curl -fsS` flags are present on both Render Deploy Hook steps: `-f` fails non-zero on HTTP error, `-s` suppresses progress meter, `-S` shows errors despite `-s` — these flags ensure the workflow step fails if Render rejects the hook call, surfacing the error clearly in the Actions log without logging the hook URL (FR-019, SC-012)

**Checkpoint**: Test failures produce downloadable artifacts; Deploy Hook errors fail the workflow step with a visible error message; no secrets appear in workflow logs

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: End-to-end validation and cleanup after all phases are complete.

- [X] T016 Run full end-to-end pipeline: open a real PR with a minor change, verify checks appear and pass within 10 minutes (SC-002), merge to main, verify Docker images tagged `sha-[SHA]` and `latest` appear in GHCR within 15 minutes (SC-003), Render services deploy and report healthy (SC-004, SC-005, SC-006)
- [X] T017 [P] Verify Neon database connectivity and Flyway migrations: check Render backend deployment logs for `Successfully applied N migration(s)` from Flyway on startup; query Neon database to confirm schema tables exist (SC-008, FR-016b)
- [X] T018 [P] Verify CORS and API connectivity: open the deployed frontend URL, perform a login or data-fetch action, confirm the network request to `VITE_API_URL` returns HTTP 200 and the UI renders data (FR-017, SC-007)
- [X] T019 [P] Replace `[GITHUB_USERNAME]` placeholder in `render.yaml` with your actual lowercase GitHub username in both `image.url` fields (plan §1.4 note) — confirm with `git grep GITHUB_USERNAME` returning zero matches before final commit

---

## Dependencies

```
T001 → T008, T009 (application-prod.yml needed by backend Docker build for prod profile)
T002 → T004, T005 (render.yaml guides Render service creation)
T003 → T008, T009 (GitHub Secrets must exist before workflows reference them)
T004 → T010 (Render services must exist before GHCR packages can be linked)
T005 → T016 (Render env vars must be set before deployment health check can pass)
T006 → T011, T013 (backend.yml must exist before verifying or amending it)
T007 → T012, T014 (frontend.yml must exist before verifying or amending it)
T008 → T010 (GHCR push must happen before setting package visibility)
T009 → T010
T008, T009 → T016 (CD jobs needed for end-to-end validation)
T015 → T016 (curl flags must be verified before end-to-end test)
T016 → T017, T018 (app must be deployed before connectivity checks)
```

## Parallel Execution Examples

**Phase 1**: T001 and T002 can be written simultaneously (independent files).

**Phase 2**: T003, T004, T005 — T004 and T005 can proceed in parallel once T002 is complete; T003 is fully independent.

**Phase 3**: T006 and T007 can be written in parallel (independent files).

**Phase 4**: T008 and T009 can be written in parallel (independent files); T010 follows both.

**Phase 5**: T011 and T012 are verification steps that can be done in parallel.

**Phase 6**: T013, T014, T015 can all be done in parallel (independent files/steps).

**Phase 7**: T017, T018, T019 can all run in parallel after T016 completes.

## Implementation Strategy

**MVP (Phases 1–4)**: Complete T001–T010. This delivers a fully functional CI/CD pipeline — PR checks run tests, merges build and push images, Render deploys from GHCR. This satisfies all P1 user stories.

**Full delivery (Phases 5–7)**: Add path-filter verification (US3), failure artifact uploads (US4), and end-to-end validation. All P2 stories covered.

**Format validation**: All 19 tasks follow the required checklist format: checkbox (`- [ ]`), sequential ID (T001–T019), optional `[P]` marker, optional `[US#]` label for story phases, description with file paths.

| Phase | Tasks | Story | Count |
|-------|-------|-------|-------|
| Setup | T001–T002 | — | 2 |
| Foundational | T003–T005 | — | 3 |
| US1 (P1) | T006–T007 | US1 | 2 |
| US2 (P1) | T008–T010 | US2 | 3 |
| US3 (P2) | T011–T012 | US3 | 2 |
| US4 (P2) | T013–T015 | US4 | 3 |
| Polish | T016–T019 | — | 4 |
| **Total** | | | **19** |
