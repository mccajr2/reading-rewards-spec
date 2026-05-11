# Data Model: CI/CD Configuration Entities

**Phase**: 1 | **Feature**: `002-deployment-cicd` | **Date**: 2026-05-10

This feature has no application-layer data model (no new database tables). The "entities" are the configuration artifacts that define the CI/CD system.

---

## Configuration Entities

### GitHub Actions Workflow

A YAML file in `.github/workflows/` that declares triggers, jobs, and steps.

| Field | Type | Value / Description |
|-------|------|---------------------|
| `name` | string | Display name in GitHub Actions UI |
| `on.push.branches` | string[] | Branches that trigger on push (e.g., `[main]`) |
| `on.push.paths` | string[] | File path globs that scope the push trigger |
| `on.pull_request.branches` | string[] | Base branches for PR triggers |
| `on.pull_request.paths` | string[] | File path globs that scope the PR trigger |
| `jobs` | map | Ordered job definitions (test → build-and-push) |
| `jobs.*.needs` | string | Job dependency (build-and-push needs test) |
| `jobs.*.if` | expression | Condition for job to run (e.g., push to main only) |
| `jobs.*.permissions` | map | GitHub token permissions (packages: write for GHCR push) |

**Instances**:
- `backend.yml` — scoped to `backend/**` and `.github/workflows/backend.yml`
- `frontend.yml` — scoped to `frontend/**` and `.github/workflows/frontend.yml`

---

### GitHub Secret

A repository-scoped encrypted value, set via GitHub → Settings → Secrets and variables → Actions.

| Secret Name | Used In | Description |
|-------------|---------|-------------|
| `RENDER_BACKEND_DEPLOY_HOOK_URL` | `backend.yml` | Full Render deploy hook URL for the backend service |
| `RENDER_FRONTEND_DEPLOY_HOOK_URL` | `frontend.yml` | Full Render deploy hook URL for the frontend service |
| `VITE_API_URL` | `frontend.yml` | Full URL to backend API, baked into frontend Docker build (e.g., `https://reading-rewards-spec-backend.onrender.com/api`) |

**Note**: `GITHUB_TOKEN` is a built-in secret; no manual setup required. It authenticates against GHCR.

---

### GHCR Docker Image

A Docker image pushed to GitHub Container Registry at `ghcr.io`.

| Field | Value |
|-------|-------|
| Backend image | `ghcr.io/[GITHUB_USERNAME]/reading-rewards-spec/backend` |
| Frontend image | `ghcr.io/[GITHUB_USERNAME]/reading-rewards-spec/frontend` |
| Tags applied | `sha-[COMMIT_SHA]` (immutable), `latest` (mutable, pulled by Render) |
| Registry auth (push) | `GITHUB_TOKEN` (GitHub Actions built-in) |
| Registry auth (pull by Render) | Package must be set to Public visibility in GitHub Packages settings |

---

### Render Service

A Render web service pulling a Docker image from GHCR.

| Field | Backend | Frontend |
|-------|---------|----------|
| `type` | `web` | `web` |
| `name` | `reading-rewards-spec-backend` | `reading-rewards-spec-frontend` |
| `env` | `docker` | `docker` |
| `plan` | `free` | `free` |
| `image.url` | `ghcr.io/[GITHUB_USERNAME]/reading-rewards-spec/backend:latest` | `ghcr.io/[GITHUB_USERNAME]/reading-rewards-spec/frontend:latest` |
| `autoDeploy` | `false` | `false` |
| `healthCheckPath` | `/actuator/health` | `/` |

---

### Render Environment Variable

Values injected into a Render service at runtime. Sensitive values are set via the Render dashboard (`sync: false` in render.yaml means "prompt user to set this manually").

| Key | Service | Value Source | Sensitive |
|-----|---------|--------------|-----------|
| `SPRING_PROFILES_ACTIVE` | Backend | `prod` (hardcoded) | No |
| `DATABASE_URL` | Backend | Neon JDBC URL (with `jdbc:` prefix) | Yes |
| `DATABASE_USER` | Backend | Neon database username | Yes |
| `DATABASE_PASSWORD` | Backend | Neon database password | Yes |
| `FRONTEND_URL` | Backend | Render frontend service URL | No |

**Note**: `VITE_API_URL` is NOT a Render environment variable — it is baked into the frontend static bundle at Docker build time in GitHub Actions.

---

### Neon Database

External serverless PostgreSQL (not represented in render.yaml).

| Property | Value |
|----------|-------|
| Provider | Neon (neon.tech) |
| Tier | Free (permanent, no expiry) |
| Connection format (Neon) | `postgresql://user:pass@ep-xxxx.region.aws.neon.tech/dbname?sslmode=require` |
| Connection format (Spring Boot) | `jdbc:postgresql://user:pass@ep-xxxx.region.aws.neon.tech/dbname?sslmode=require` |
| Schema management | Flyway migrations on backend startup (`spring.flyway.enabled: true` in `application-prod.yml`) |
| Migration location | `backend/src/main/resources/db/migration/` |

---

### Spring Boot Production Profile

File: `backend/src/main/resources/application-prod.yml`

Activated when `SPRING_PROFILES_ACTIVE=prod` is set on the Render backend service. Overrides the base `application.yml`.

| Setting | Base (`application.yml`) | Prod (`application-prod.yml`) |
|---------|--------------------------|-------------------------------|
| `spring.datasource.url` | H2 in-memory | `${DATABASE_URL}` (Neon JDBC) |
| `spring.datasource.username` | `sa` | `${DATABASE_USER}` |
| `spring.datasource.password` | (empty) | `${DATABASE_PASSWORD}` |
| `spring.jpa.hibernate.ddl-auto` | `create-drop` | `validate` |
| `spring.flyway.enabled` | `false` | `true` |
| `spring.flyway.locations` | — | `classpath:db/migration` |
| `frontend.url` | `http://localhost:3000` | `${FRONTEND_URL}` |
