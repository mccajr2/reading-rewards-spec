# Deployment Complete: Reading Rewards Spec (v0.2.0-deployed)

**Date Completed**: 2026-05-11  
**Feature**: `002-deployment-cicd`  
**Status**: ✅ **COMPLETE** — All 19 tasks delivered, live in production

---

## Live Deployment URLs

- **Frontend**: https://reading-rewards-spec-frontend.onrender.com
- **Backend API**: https://reading-rewards-spec-backend.onrender.com/api
- **Health Check**: https://reading-rewards-spec-backend.onrender.com/actuator/health

---

## What Was Delivered

### ✅ Core Infrastructure

| Component | Technology | Status |
|-----------|-----------|--------|
| Container Registry | GitHub Container Registry (GHCR) | ✅ Live |
| CI/CD Pipeline | GitHub Actions | ✅ Live |
| Deployment Platform | Render Web Services | ✅ Live |
| Database | Neon Serverless PostgreSQL 17 | ✅ Live |
| API Framework | Spring Boot 3.5.6 (Java 21) | ✅ Live |
| Frontend Framework | React 18 + Vite 7 (Node 22) | ✅ Live |

### ✅ CI/CD Features

- **Path-triggered workflows**: Backend and frontend tests/deploys only when their respective code changes
- **Automated Docker builds**: GitHub Actions builds images once, pushes to GHCR
- **Render deploy hooks**: Triggered after successful GHCR push (only on main branch)
- **Test gates**: All tests must pass before Docker build and deploy are triggered
- **Artifact preservation**: Failed test reports uploaded to GitHub Actions artifacts

### ✅ Production Configuration

- **Spring Boot production profile**: `application-prod.yml` with PostgreSQL, Flyway, and prod-level settings
- **CORS configuration**: Dynamically configured from `FRONTEND_URL` environment variable
- **Email service resilience**: AuthController gracefully handles missing email service bean
- **Database migrations**: Flyway V1__init.sql runs automatically on startup
- **Health checks**: `/actuator/health` (backend) and `/` (frontend) configured on Render

### ✅ Documented and Runbooks

- **Quickstart guide**: Step-by-step setup instructions in `specs/002-deployment-cicd/quickstart.md`
- **Research decisions**: Architecture rationale in `specs/002-deployment-cicd/research.md`
- **Task checklist**: All 19 tasks verified complete in `specs/002-deployment-cicd/tasks.md`
- **Data model**: Entity relationships documented in `specs/002-deployment-cicd/data-model.md`

---

## Key Technical Decisions

1. **GHCR over Docker Hub**: Zero-friction authentication via `GITHUB_TOKEN`, no rate limits, native namespace integration
2. **Deploy Hooks over Render Auto-Deploy**: GitHub Actions owns the CI gate; tests must pass before deploy
3. **Single JDBC URL secret**: Full `jdbc:postgresql://...` stored as `DATABASE_URL`, no runtime transformation
4. **Flyway migrations**: Automatic schema initialization on prod startup, with `ddl-auto: validate` for safety
5. **Dynamic CORS origins**: Frontend URL read from environment, allowing dev and prod origins simultaneously

---

## Security Notes

### Secrets Currently Configured

**GitHub Repository Secrets** (in Settings → Secrets and variables → Actions):
- `RENDER_BACKEND_DEPLOY_HOOK_URL` ✅
- `RENDER_FRONTEND_DEPLOY_HOOK_URL` ✅
- `VITE_API_URL` (baked into frontend Docker image at build time) ✅

**Render Backend Environment Variables** (in service dashboard):
- `SPRING_PROFILES_ACTIVE=prod` ✅
- `DATABASE_URL` (JDBC URL to Neon) ✅
- `DATABASE_USER` (Neon username) ✅
- `DATABASE_PASSWORD` (Neon password) ✅
- `FRONTEND_URL` (Render frontend URL for CORS) ✅
- `BREVO_API_KEY` (email service, currently optional) ✅
- `SPRING_MAIL_FROM` (email sender, currently optional) ✅

### Secret Exposure Warning

⚠️ **The Brevo API key was inadvertently exposed in chat context**. It has been rotated in the Brevo console. The old key in `.env.sh` is now invalid and can be safely deleted or left as documentation.

### Best Practices Applied

- ✅ No secrets in code or `.env.sh` are committed to Git (protected by `.env*` in `.gitignore`)
- ✅ GHCR packages are Public (Render can pull without credentials)
- ✅ Database URL includes `channel_binding=require` for Neon security
- ✅ `ddl-auto: validate` prevents accidental schema modifications in production
- ✅ Environment variables are synced: false in `render.yaml` (set manually for secrets safety)

---

## How to Use This Deployment

### Deploy a New Change

1. Make code changes in `backend/` or `frontend/` directory
2. Create a pull request → GitHub Actions runs tests
3. If tests pass and PR is approved, merge to `main`
4. GitHub Actions automatically:
   - Runs tests again
   - Builds Docker images
   - Pushes to GHCR with tags `sha-[COMMIT_SHA]` and `latest`
   - Calls Render deploy hook → Render pulls new `latest` image and redeploys
5. Within 2–3 minutes, new code is live

### Manual Restart

If you need to restart the services without deploying new code:
1. Go to Render dashboard → Service → Manual Deploy button
2. Or trigger by pushing an empty commit: `git commit --allow-empty -m "chore: trigger render redeploy"`

### View Logs

- **Backend**: Render dashboard → Service → Logs tab (real-time tail)
- **Frontend**: Render dashboard → Service → Logs tab
- **GitHub Actions**: GitHub repo → Actions tab (workflow run history)

### Run Tests Locally

```bash
# Backend
cd backend && ./mvnw test

# Frontend
cd frontend && npm test
```

---

## What's Ready for the Next Feature

- ✅ CI/CD pipeline is automated and requires zero manual intervention per deployment
- ✅ All infrastructure is serverless/free-tier (Neon free tier, Render free tier)
- ✅ Local development uses H2 in-memory database for zero Docker overhead
- ✅ Production database is persistent and never expires (unlike Render's 30-day Postgres)
- ✅ Spring Boot test suite (32/32 passing) can be extended with new feature tests
- ✅ Frontend test suite (31/31 passing with Vitest) can be extended with new E2E tests

---

## Commits in This Cycle

```
92467a1 (HEAD -> main, tag: v0.2.0-deployed) 
    docs(spec): mark all 002-deployment-cicd tasks complete, fix CORS and email service resilience

ec681db (origin/main) 
    fix(backend): allow Render frontend origin for CORS and harden email-service injection

10a0152 
    fix(render): bind backend to PORT and remove invalid nginx backend proxy

2059b95 
    fix(ci): use env context for optional Render deploy hooks
```

---

## Next Steps (Tomorrow's New Feature)

The deployment scaffold is complete and stable. Next features can now focus on:

1. **User Story: Parent/Child Account Linking** (feature `003-parent-child-accounts`)
2. **User Story: Book Scanning & ISBN Lookup** (feature `004-book-scanning`)
3. **User Story: Reading Challenges** (feature `005-reading-challenges`)
4. **Operational: Performance & Observability** (feature `006-monitoring`)

All of these can leverage the automated deployment pipeline that is now in place.

---

**End of Deployment Report**  
Ready for next feature spec tomorrow! 🚀
