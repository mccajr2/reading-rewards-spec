# Quickstart: CI/CD Setup

**Feature**: `002-deployment-cicd` | **Date**: 2026-05-10

This guide walks through the one-time setup required to activate GitHub Actions CI/CD and Render deployment for reading-rewards-spec. Follow these steps in order.

---

## Prerequisites

- GitHub repository for `reading-rewards-spec` exists and you have admin access
- Render account created at [render.com](https://render.com)
- Neon account created at [neon.tech](https://neon.tech)
- Docker installed locally (for validating Dockerfiles, optional)

---

## Step 1: Provision Neon PostgreSQL Database

1. Log in to [Neon console](https://console.neon.tech)
2. Create a new project → name it `reading-rewards-spec`
3. Note the connection string from the dashboard. It looks like:
   ```
   postgresql://neondb_owner:abc123@ep-xxxx.us-east-2.aws.neon.tech/neondb?sslmode=require
   ```
4. Prepend `jdbc:` to create the Spring Boot JDBC URL:
   ```
   jdbc:postgresql://neondb_owner:abc123@ep-xxxx.us-east-2.aws.neon.tech/neondb?sslmode=require
   ```
5. Note the username (`neondb_owner`) and password separately.

---

## Step 2: Create Render Services

1. Log in to [Render dashboard](https://dashboard.render.com)
2. Click **New → Web Service**
3. Choose **Deploy an existing image from a registry**
4. Set the image URL to:
   ```
   ghcr.io/[GITHUB_USERNAME]/reading-rewards-spec/backend:latest
   ```
   Replace `[GITHUB_USERNAME]` with your GitHub username (lowercase).
5. Configure the backend service:
   - **Name**: `reading-rewards-spec-backend`
   - **Plan**: Free
   - **Health check path**: `/actuator/health`
   - **Environment variables** (add each):
     - `SPRING_PROFILES_ACTIVE` = `prod`
     - `DATABASE_URL` = `jdbc:postgresql://...` (from Step 1, full JDBC URL)
     - `DATABASE_USER` = your Neon username
     - `DATABASE_PASSWORD` = your Neon password
     - `FRONTEND_URL` = `https://reading-rewards-spec-frontend.onrender.com` (set after creating frontend service)
6. Repeat for the frontend service:
   - **Image URL**: `ghcr.io/[GITHUB_USERNAME]/reading-rewards-spec/frontend:latest`
   - **Name**: `reading-rewards-spec-frontend`
   - **Plan**: Free
   - No environment variables needed (VITE_API_URL is baked in at build time)
7. For each service, navigate to **Settings → Deploy Hook** and copy the URL.

---

## Step 3: Set GHCR Package Visibility to Public

After the first GitHub Actions run pushes images to GHCR:

1. Go to your GitHub profile → **Packages**
2. Find `reading-rewards-spec/backend` and `reading-rewards-spec/frontend`
3. For each package → **Package settings → Change visibility → Public**

This allows Render to pull images without authentication credentials.

> **If your repository is private**: Instead of making packages public, create a GitHub Personal Access Token with `read:packages` scope and configure Render's registry credentials in the service dashboard.

---

## Step 4: Add GitHub Secrets

In the GitHub repository, go to **Settings → Secrets and variables → Actions → New repository secret** and add:

| Secret Name | Value |
|-------------|-------|
| `RENDER_BACKEND_DEPLOY_HOOK_URL` | Full deploy hook URL from Render backend service settings |
| `RENDER_FRONTEND_DEPLOY_HOOK_URL` | Full deploy hook URL from Render frontend service settings |
| `VITE_API_URL` | `https://reading-rewards-spec-backend.onrender.com/api` |

> `GITHUB_TOKEN` is built-in — no setup needed.

---

## Step 5: Add Application Files

Two files need to be added to the repository before the first deployment:

### 5a. Create `backend/src/main/resources/application-prod.yml`

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

### 5b. Create `.github/workflows/backend.yml`

```yaml
name: Backend

on:
  push:
    branches: [main]
    paths:
      - 'backend/**'
      - '.github/workflows/backend.yml'
  pull_request:
    branches: [main]
    paths:
      - 'backend/**'
      - '.github/workflows/backend.yml'

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}/backend

jobs:
  test:
    name: Test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Run backend tests
        working-directory: backend
        run: ./mvnw test

      - name: Upload test results on failure
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: backend-test-results
          path: backend/target/surefire-reports/

  build-and-push:
    name: Build & Deploy
    needs: test
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - name: Log in to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract image metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=sha,prefix=sha-
            type=raw,value=latest

      - name: Build and push backend image
        uses: docker/build-push-action@v6
        with:
          context: ./backend
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}

      - name: Trigger Render backend deploy
        run: curl -fsS -X POST "${{ secrets.RENDER_BACKEND_DEPLOY_HOOK_URL }}"
```

### 5c. Create `.github/workflows/frontend.yml`

```yaml
name: Frontend

on:
  push:
    branches: [main]
    paths:
      - 'frontend/**'
      - '.github/workflows/frontend.yml'
  pull_request:
    branches: [main]
    paths:
      - 'frontend/**'
      - '.github/workflows/frontend.yml'

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}/frontend

jobs:
  test:
    name: Test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Node.js 22
        uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        working-directory: frontend
        run: npm ci

      - name: Run frontend tests
        working-directory: frontend
        run: npm test

      - name: Upload test results on failure
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: frontend-test-results
          path: frontend/test-results/

  build-and-push:
    name: Build & Deploy
    needs: test
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - name: Log in to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract image metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=sha,prefix=sha-
            type=raw,value=latest

      - name: Build and push frontend image
        uses: docker/build-push-action@v6
        with:
          context: ./frontend
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          build-args: |
            VITE_API_URL=${{ secrets.VITE_API_URL }}

      - name: Trigger Render frontend deploy
        run: curl -fsS -X POST "${{ secrets.RENDER_FRONTEND_DEPLOY_HOOK_URL }}"
```

### 5d. Create `render.yaml` in repository root

Replace `[GITHUB_USERNAME]` with your actual GitHub username (all lowercase).

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

---

## Step 6: Push Changes and Verify

1. Commit and push all new files to `main`:
   ```bash
   git add .github/workflows/ backend/src/main/resources/application-prod.yml render.yaml
   git commit -m "feat: add GitHub Actions CI/CD and Render deployment config"
   git push origin main
   ```
2. Open GitHub → **Actions** tab and verify both `Backend` and `Frontend` workflows trigger.
3. Watch the `test` job complete, then `build-and-push` push images to GHCR.
4. Verify in GitHub → **Packages** that `reading-rewards-spec/backend` and `reading-rewards-spec/frontend` packages appear.
5. Set package visibility to Public (Step 3).
6. Open Render dashboard → verify both services show a new deploy in progress.
7. Once deploys complete, open the frontend URL and verify the app loads and can reach the backend API.

---

## Verification Checklist

- [ ] Neon database provisioned; JDBC URL constructed with `jdbc:` prefix
- [ ] Render backend service created with correct GHCR image URL and env vars
- [ ] Render frontend service created with correct GHCR image URL
- [ ] Render deploy hook URLs copied for both services
- [ ] GitHub Secrets set: `RENDER_BACKEND_DEPLOY_HOOK_URL`, `RENDER_FRONTEND_DEPLOY_HOOK_URL`, `VITE_API_URL`
- [ ] `application-prod.yml` created and committed
- [ ] `.github/workflows/backend.yml` created and committed
- [ ] `.github/workflows/frontend.yml` created and committed
- [ ] `render.yaml` created with `[GITHUB_USERNAME]` replaced
- [ ] GitHub Actions workflows triggered and passed on first push
- [ ] GHCR packages set to Public visibility
- [ ] Render services deployed and health checks passing
- [ ] Frontend loads at Render URL and API calls succeed

---

## Troubleshooting

### Backend fails to start on Render

Check Render service logs. Common causes:
- **Database connection refused**: Verify `DATABASE_URL` has `jdbc:` prefix and Neon endpoint is correct
- **Flyway migration failure**: Check `db/migration/` SQL scripts for syntax errors; inspect logs for migration version conflict
- **Missing environment variable**: Render logs will show `Could not resolve placeholder '...'` — verify all env vars are set in Render dashboard

### Frontend cannot reach backend API

- Open browser DevTools → Network tab → look for failed API requests
- Verify `VITE_API_URL` secret was set **before** the GitHub Actions build that pushed the image (VITE values are baked at build time)
- If VITE_API_URL was wrong, update the secret and re-trigger the workflow (push a trivial frontend change)

### GitHub Actions fails at "Trigger Render deploy"

- `curl: (22) The requested URL returned error: 401` → Deploy hook URL is invalid or expired. Regenerate in Render dashboard and update the GitHub Secret.
- `curl: (22) The requested URL returned error: 404` → Service was deleted or hook is disabled.

### Render cannot pull GHCR image

- Error: `Error response from daemon: unauthorized`
- Solution: Set the GHCR package to Public (GitHub → Packages → Package settings → Change visibility → Public)
