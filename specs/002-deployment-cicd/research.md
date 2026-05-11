# Research: Render Deployment with GitHub Actions CI/CD

**Phase**: 0 | **Feature**: `002-deployment-cicd` | **Date**: 2026-05-10

## Decision Log

### 1. Container Registry: GHCR vs Docker Hub

**Decision**: GitHub Container Registry (`ghcr.io`)  
**Rationale**: Authenticates natively with `GITHUB_TOKEN` (built-in, zero secret setup). Images live in the same GitHub namespace as the source repo. Free for public repositories and included in GitHub Free for private repositories up to 500 MB/month. No separate account required.  
**Alternatives considered**: Docker Hub requires a separate account, DOCKER_USERNAME + DOCKER_PASSWORD secrets, and is rate-limited for anonymous/free pulls. GHCR has no pull rate limits for authenticated GitHub Actions.

### 2. Render Deploy Trigger: Render Deploy Hooks vs Render GitHub Auto-Deploy

**Decision**: GitHub Actions calls Render Deploy Hooks after pushing images to GHCR  
**Rationale**: GitHub Actions owns the CI gate — tests must pass before a deploy is triggered. With Render auto-deploy from GitHub, Render monitors the repo directly and would deploy even if GitHub Actions has not run yet, or would trigger its own Docker build (ignoring the GHCR image). Deploy Hooks give GitHub Actions full control: build → test → push → deploy is an explicit, ordered sequence.  
**Alternatives considered**: Render auto-deploy (`autoDeploy: true` + `dockerfilePath`) is simpler but bypasses GitHub Actions' test gate. It also duplicates the Docker build (GitHub Actions builds for GHCR; Render rebuilds from scratch). Using Deploy Hooks eliminates duplicate builds and keeps the pipeline deterministic.

### 3. Render Image Source: GHCR Pull vs Render-Built Image

**Decision**: Render services pull pre-built Docker images from GHCR  
**Rationale**: Images built once in GitHub Actions are the exact same artifact deployed to Render. This is the standard "build once, deploy anywhere" principle. Render pulling from GHCR means no rebuild time on Render's side and a deterministic artifact.  
**Alternatives considered**: Render rebuilding from `dockerfilePath` is simpler to configure (no `image.url` needed) but violates build-once semantics and adds 3–5 minutes of rebuild time on Render.

### 4. Neon JDBC URL Format

**Decision**: Store the full JDBC URL (`jdbc:postgresql://...`) in the `DATABASE_URL` GitHub Secret and Render environment variable  
**Rationale**: Spring Boot's `spring.datasource.url` requires the `jdbc:` scheme prefix. Neon provides connection strings in `postgresql://` format. Converting at secret-storage time (manually prefixing `jdbc:`) is the least-surprise approach — no runtime string manipulation, no Spring configuration magic.  
**Alternatives considered**: Storing the raw `postgresql://` URL and transforming it at startup via an `application-prod.yml` expression (`jdbc:${DATABASE_URL}`) is fragile if the value already contains `jdbc:`. Using Spring's datasource auto-configuration with separate host/port/name/user/password properties requires four secrets instead of one. Prefixing at secret time is the simplest and most explicit.  
**Action**: When creating the `DATABASE_URL` GitHub Secret and Render env var, prepend `jdbc:` to the Neon-provided connection string. Example:  
`jdbc:postgresql://ep-xxxx.us-east-2.aws.neon.tech/neondb?sslmode=require`

### 5. application-prod.yml: What the Production Profile Must Set

**Decision**: Create `backend/src/main/resources/application-prod.yml` that enables Flyway and sets `ddl-auto: validate`  
**Rationale**: The base `application.yml` disables Flyway (`flyway.enabled: false`) and uses `create-drop` for H2 dev convenience. These settings must be overridden in production. The `prod` profile is activated via `SPRING_PROFILES_ACTIVE=prod` on Render.  
**Content to create**:
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

### 6. GHCR Image Visibility for Render Pull Access

**Decision**: Set GHCR packages to public visibility after first push (or configure Render with a PAT for private pull)  
**Rationale**: Render's free tier does not support private registry authentication natively via render.yaml. The safest and simplest path for a personal project is to make GHCR packages public (GitHub → Packages → Change visibility → Public). Public packages are free and unlimited.  
**Alternative**: If the repo is private and packages must remain private, create a GitHub Personal Access Token with `read:packages` scope, store it as a Render environment secret, and configure Render's registry credentials. This is a manual dashboard step not representable in render.yaml. Not recommended for the current project scope.

### 7. Path-Triggered Workflows: Separate Files vs One File with Jobs

**Decision**: Two separate workflow files (`backend.yml`, `frontend.yml`), each with `on.push.paths` and `on.pull_request.paths` filters  
**Rationale**: Separate files give clean separation — each file is independently readable, independently triggerable from the GitHub Actions UI, and produces separate check names in the PR status interface. Path filters are simpler to reason about when scoped to a single service.  
**Alternatives considered**: One `ci.yml` with conditional job steps and a root-level path filter (`paths: ['backend/**', 'frontend/**']`) triggers both jobs on any change. One `ci.yml` with separate jobs each having their own path filter uses `github.event.head_commit.modified` filtering — complex and error-prone. Separate files are the industry standard for monorepo CI.

### 8. Frontend VITE_API_URL Baking

**Decision**: Pass `VITE_API_URL` as a Docker `--build-arg` during GitHub Actions build, stored as a GitHub Secret  
**Rationale**: Vite replaces `import.meta.env.VITE_*` at build time. The value is embedded in the compiled static bundle. It cannot be injected at runtime via `docker run -e`. The Render frontend service URL for the backend is stable (set once in Render dashboard), so baking it at build time is correct. The value is stored as `VITE_API_URL` GitHub Secret (e.g., `https://reading-rewards-spec-backend.onrender.com/api`).  
**Alternatives considered**: Using a runtime-injected config via a shell script that generates `window.env.js` before nginx starts avoids the bake-in limitation but adds complexity (entrypoint scripting, Dockerfile changes). Not warranted for a personal project where the backend URL is stable.

### 9. Render Health Checks

**Decision**: Backend health check path is `/actuator/health`; frontend health check is `/` (default)  
**Rationale**: `spring-boot-starter-actuator` is already in `pom.xml`. The base `application.yml` already exposes the `health` endpoint (`management.endpoints.web.exposure.include: health,info`). No code changes are needed for the backend health check. Nginx serves `/` with HTTP 200 for the frontend — no explicit health check config required.

### 10. render.yaml Adaptation from reading-rewards

**Decision**: Adapt `reading-rewards/render.yaml` by removing the Postgres service (using Neon instead) and changing `dockerfilePath`/`autoDeploy` to `image.url` with `autoDeploy: false`  
**Rationale**: The original render.yaml includes a Render Postgres service (which expires after 30 days on the free tier) and uses Render-triggered Docker builds. Both must change. The Neon database is external and not represented in render.yaml. Services use GHCR images instead of Render builds.
