# Implementation Plan: Render Deployment with GitHub Actions CI/CD

**Branch**: `002-deployment-cicd` | **Date**: 2026-05-10 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `/specs/002-deployment-cicd/spec.md`

## Summary

Set up GitHub Actions CI/CD for the reading-rewards-spec Spring Boot + React monorepo. Two path-triggered workflows (`backend.yml`, `frontend.yml`) run tests on every pull request to main and — on merge to main — build Docker images, push them to GitHub Container Registry (GHCR), then call Render Deploy Hooks to trigger deployment. The PostgreSQL database is hosted permanently on Neon (free tier, not Render Postgres). Flyway migrations run on backend startup. All sensitive values are stored as GitHub Secrets or Render environment variables; no credentials appear in tracked files.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.9 / Node.js 22 (frontend)  
**Primary Dependencies**: Spring Boot 3.5.6 + Maven Wrapper (backend CI), Vitest + npm (frontend CI), Docker, GitHub Actions, GitHub Container Registry (`ghcr.io`), Render (Docker web services), Neon (serverless PostgreSQL)  
**Storage**: Neon serverless PostgreSQL; JDBC URL format `jdbc:postgresql://...` (see research.md §4)  
**Testing**: JUnit 5 / Maven Surefire (`./mvnw test`) for backend; Vitest (`npm test`) for frontend  
**Target Platform**: Render free tier — two Docker web services (backend + frontend); no Render Postgres  
**Project Type**: CI/CD infrastructure for a full-stack web application  
**Performance Goals**: PR checks complete within 10 minutes (SC-002); end-to-end deploy pipeline within 15 minutes (SC-006)  
**Constraints**: Free-tier Render and Neon; GHCR as registry; Neon JDBC URL must carry `jdbc:` prefix; no secrets in tracked files; `VITE_API_URL` baked at Docker build time  
**Scale/Scope**: Two Render services + one external Neon database; single GitHub repository; no Kubernetes or multi-environment setup

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Note |
|-----------|--------|------|
| I. Behavior Parity | ✅ PASS | CI/CD infrastructure only; no externally visible endpoint behavior changes |
| II. Spec Before Structure | ✅ PASS | This plan precedes all workflow and config file creation |
| III. Tests Are Delivery Criteria | ✅ PASS | Backend and frontend tests are mandatory gates; workflows fail fast if tests fail; merge is blocked on failure |
| IV. Clean Architecture | ✅ PASS | Workflow files live in `.github/workflows/`; no coupling to application logic |
| V. Secure Configuration | ✅ PASS | All secrets via GitHub Secrets; `render.yaml` uses `sync: false` for sensitive values; no live credentials in tracked files |

**Post-design re-check**: ✅ All gates still pass. `application-prod.yml` uses `${ENV_VAR}` placeholders only; `render.yaml` uses `sync: false`; workflow files reference `${{ secrets.* }}` only.

## Project Structure

### Documentation (this feature)

```text
specs/002-deployment-cicd/
├── plan.md              # This file
├── research.md          # Phase 0 — decisions on registry, deploy strategy, JDBC URL, etc.
├── data-model.md        # Phase 1 — configuration entity model
├── quickstart.md        # Phase 1 — step-by-step first-time setup guide
└── contracts/
    ├── health-check.md  # /actuator/health endpoint contract
    └── deploy-hook.md   # Render Deploy Hook API contract
```

### Source Code Changes

```text
.github/
└── workflows/
    ├── backend.yml        # Backend CI (tests on PR) + CD (build/push/deploy on main push)
    └── frontend.yml       # Frontend CI (tests on PR) + CD (build/push/deploy on main push)

backend/
└── src/main/resources/
    └── application-prod.yml   # New: production Spring profile (Flyway enabled, ddl-auto: validate)

render.yaml                    # New: Render service declarations using GHCR images (no Render Postgres)
```

**Structure decision**: Two separate workflow files, one per service, path-filtered so each triggers only when its own service files change. This satisfies FR-003/FR-004 cleanly without conditional job logic.

---

## Phase 0: Research Summary

All unknowns resolved. See [research.md](research.md) for full decision rationale.

| Topic | Decision |
|-------|----------|
| Container registry | **GHCR** (`ghcr.io`) — native `GITHUB_TOKEN` auth, no extra secrets |
| Deploy trigger | **Render Deploy Hooks** called by GitHub Actions after GHCR push |
| Render image source | **Pre-built GHCR image** (`image.url` in render.yaml), not Render-built from Dockerfile |
| Neon JDBC URL | Store as `jdbc:postgresql://...` in secrets (prepend `jdbc:` to Neon's `postgresql://` string) |
| `application-prod.yml` | Must be created: enables Flyway, sets `ddl-auto: validate`, reads env vars |
| GHCR package visibility | Set to **Public** in GitHub Packages settings after first push (Render pull access) |
| Workflow structure | **Two separate files** — `backend.yml` and `frontend.yml` |
| `VITE_API_URL` | Stored as GitHub Secret; passed as Docker `--build-arg` at build time |
| Backend health check | `/actuator/health` — already configured in `application.yml`, no code change needed |
| `render.yaml` adaptation | Remove Render Postgres service; replace `dockerfilePath` with `image.url`; set `autoDeploy: false` |

---

## Phase 1: Design

### 1.1 New File: `backend/src/main/resources/application-prod.yml`

Activated when `SPRING_PROFILES_ACTIVE=prod`. Overrides the base `application.yml` for Render deployment.

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
    locations: classpath:db/migration

frontend:
  url: ${FRONTEND_URL}
```

**Why**: Base `application.yml` uses H2, disables Flyway, and uses `create-drop` — all dev-only settings. The prod profile overrides these to use Neon and run Flyway migrations on startup.

---

### 1.2 New File: `.github/workflows/backend.yml`

**Trigger**: Push to `main` or PR to `main` — only when `backend/**` or `.github/workflows/backend.yml` changes.

**Jobs**:

1. **`test`** (runs on all triggers)
   - Checkout code
   - Set up Java 21 (temurin, Maven cache)
   - `./mvnw test` in `backend/` directory
   - Upload `surefire-reports/` artifact on failure

2. **`build-and-push`** (runs only on `push` to `main`, depends on `test`)
   - Login to GHCR using `GITHUB_TOKEN`
   - Generate image tags: `sha-[COMMIT_SHA]` and `latest` using `docker/metadata-action`
   - Build and push `./backend` Docker context to `ghcr.io/${{ github.repository }}/backend`
   - `curl -fsS -X POST "${{ secrets.RENDER_BACKEND_DEPLOY_HOOK_URL }}"`

**Key design decisions**:
- `permissions: packages: write` on the `build-and-push` job (required for GHCR push)
- `if: github.ref == 'refs/heads/main' && github.event_name == 'push'` guards the deploy job — PRs only run tests
- `-fsS` on curl: fails non-zero on HTTP error (`-f`), suppresses progress (`-s`), shows errors (`-S`) — URL not logged

---

### 1.3 New File: `.github/workflows/frontend.yml`

**Trigger**: Push to `main` or PR to `main` — only when `frontend/**` or `.github/workflows/frontend.yml` changes.

**Jobs**:

1. **`test`** (runs on all triggers)
   - Checkout code
   - Set up Node 22 with npm cache scoped to `frontend/package-lock.json`
   - `npm ci` in `frontend/` directory
   - `npm test` in `frontend/` directory
   - Upload test results artifact on failure

2. **`build-and-push`** (runs only on `push` to `main`, depends on `test`)
   - Login to GHCR using `GITHUB_TOKEN`
   - Generate image tags: `sha-[COMMIT_SHA]` and `latest`
   - Build and push `./frontend` Docker context with `build-args: VITE_API_URL=${{ secrets.VITE_API_URL }}`
   - `curl -fsS -X POST "${{ secrets.RENDER_FRONTEND_DEPLOY_HOOK_URL }}"`

**Key design decision**: `build-args: VITE_API_URL=...` is mandatory. The Vite build in the Dockerfile calls `npm run build` which replaces `import.meta.env.VITE_API_URL` at compile time. Runtime injection via `docker run -e` does not work for Vite static bundles.

---

### 1.4 New File: `render.yaml`

Adapted from `reading-rewards/render.yaml`. Key changes:
- **Postgres service removed** — Neon is the database (external, not managed by Render)
- **`dockerfilePath` replaced with `image.url`** — Render pulls pre-built GHCR images instead of building
- **`autoDeploy: false`** — GitHub Actions controls deploy timing via Deploy Hooks, not Render auto-detection
- **Service names updated** — `reading-rewards-spec-backend` / `reading-rewards-spec-frontend`

```yaml
services:
  - type: web
    name: reading-rewards-spec-backend
    env: docker
    plan: free
    image:
      url: ghcr.io/[GITHUB_USERNAME]/reading-rewards-spec/backend:latest
    autoDeploy: false
    healthCheckPath: /actuator/health
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: prod
      - key: DATABASE_URL
        sync: false
      - key: DATABASE_USER
        sync: false
      - key: DATABASE_PASSWORD
        sync: false
      - key: FRONTEND_URL
        sync: false

  - type: web
    name: reading-rewards-spec-frontend
    env: docker
    plan: free
    image:
      url: ghcr.io/[GITHUB_USERNAME]/reading-rewards-spec/frontend:latest
    autoDeploy: false
    healthCheckPath: /
```

> **Note**: Replace `[GITHUB_USERNAME]` with your actual GitHub username (lowercase) before committing.

---

### 1.5 GitHub Secrets Required

| Secret Name | Value | Set in |
|-------------|-------|--------|
| `RENDER_BACKEND_DEPLOY_HOOK_URL` | Render deploy hook URL for backend | GitHub → Settings → Secrets |
| `RENDER_FRONTEND_DEPLOY_HOOK_URL` | Render deploy hook URL for frontend | GitHub → Settings → Secrets |
| `VITE_API_URL` | `https://reading-rewards-spec-backend.onrender.com/api` | GitHub → Settings → Secrets |
| `GITHUB_TOKEN` | Built-in — no setup required | Auto-injected by GitHub Actions |

**Render environment variables** (set in Render dashboard or via render.yaml `sync: false` prompt):

| Key | Service | Value |
|-----|---------|-------|
| `SPRING_PROFILES_ACTIVE` | Backend | `prod` |
| `DATABASE_URL` | Backend | `jdbc:postgresql://...` (Neon JDBC URL with `jdbc:` prefix) |
| `DATABASE_USER` | Backend | Neon username |
| `DATABASE_PASSWORD` | Backend | Neon password |
| `FRONTEND_URL` | Backend | `https://reading-rewards-spec-frontend.onrender.com` |

---

## Acceptance Coverage

| FR / SC | Implementation |
|---------|---------------|
| FR-001 | `backend.yml` `test` job runs on all PRs to main for backend paths |
| FR-002 | `frontend.yml` `test` job runs on all PRs to main for frontend paths |
| FR-003 | `backend.yml` `on.push.paths` + `on.pull_request.paths` = `backend/**` |
| FR-004 | `frontend.yml` `on.push.paths` + `on.pull_request.paths` = `frontend/**` |
| FR-005 | `build-and-push` job in both workflows builds Docker images on main push |
| FR-006 | `docker/metadata-action` tags with `sha-[SHA]` and `latest`; pushed to GHCR |
| FR-007 | Registry login uses `GITHUB_TOKEN` (built-in); no hardcoded credentials |
| FR-008 | `if: github.ref == 'refs/heads/main' && github.event_name == 'push'` guards both deploy jobs |
| FR-009 | Render services pull from GHCR; Neon is the database (no Render Postgres) |
| FR-010 | Backend `envVars` in render.yaml: `SPRING_PROFILES_ACTIVE`, `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `FRONTEND_URL` |
| FR-011 | `VITE_API_URL` baked into frontend Docker image via `--build-arg` in GitHub Actions |
| FR-012 | Spring Boot Actuator already configured; `management.endpoints.web.exposure.include: health,info` |
| FR-013 | `healthCheckPath: /actuator/health` in render.yaml backend service |
| FR-014 | `healthCheckPath: /` in render.yaml frontend service (nginx returns 200 by default) |
| FR-015 | `build-and-push` job has `needs: test` — deploy is blocked if tests fail |
| FR-016 | Neon database used; no Render Postgres service in render.yaml |
| FR-016b | `spring.flyway.enabled: true` in `application-prod.yml`; migrations in `db/migration/` |
| FR-017 | `VITE_API_URL` points to backend Render URL; nginx config passes API requests through |
| FR-018 | Dockerfiles already validated (multi-stage builds present and functional) |
| FR-019 | `upload-artifact` on test failure; curl `-f` flag fails the deploy step on Render hook error |

---

## Complexity Tracking

No constitution violations. All decisions use the simplest approach that satisfies requirements.
