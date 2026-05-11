# Contract: Render Deploy Hook

**Feature**: `002-deployment-cicd` | **Date**: 2026-05-10

## Overview

Render Deploy Hooks are HTTPS endpoints that GitHub Actions calls (via HTTP POST) to trigger a new deployment of a Render service. GitHub Actions calls the hook **after** pushing updated Docker images to GHCR, so Render pulls the latest image and restarts the service.

---

## Endpoint

```
POST https://api.render.com/deploy/srv-[SERVICE_ID]?key=[DEPLOY_KEY]
```

The full URL is provided by Render when you set up a Deploy Hook in the Render dashboard. It is opaque — the `SERVICE_ID` and `DEPLOY_KEY` are Render-internal values.

---

## Request

| Property | Value |
|----------|-------|
| Method | `POST` |
| Body | Empty (no body required) |
| Authentication | Embedded in URL query parameter (`key`) |
| Content-Type | Not required |

**GitHub Actions invocation**:

```yaml
- name: Trigger Render deploy
  run: curl -fsS -X POST "${{ secrets.RENDER_BACKEND_DEPLOY_HOOK_URL }}"
```

The `-f` flag makes `curl` exit with a non-zero code if the HTTP response is an error (4xx/5xx), causing the GitHub Actions step to fail.

---

## Response

**HTTP Status**: `200 OK` (deploy triggered successfully)

```json
{
  "id": "dep-xxxx",
  "status": "created"
}
```

A `200` response means Render has **queued** the deploy, not that the deploy is complete. Render builds/pulls the image and starts health checks asynchronously after this response.

**Error responses**:
- `401 Unauthorized`: Invalid or missing deploy key
- `404 Not Found`: Service ID does not exist or hook is disabled

---

## Secrets

Each Render service has a unique deploy hook URL. Store them as GitHub Secrets:

| Secret Name | Service |
|-------------|---------|
| `RENDER_BACKEND_DEPLOY_HOOK_URL` | `reading-rewards-spec-backend` |
| `RENDER_FRONTEND_DEPLOY_HOOK_URL` | `reading-rewards-spec-frontend` |

**How to obtain**:
1. Go to Render dashboard → select the service
2. Navigate to Settings → Deploy Hook
3. Copy the full URL
4. Add it as a GitHub Secret in the repository settings

---

## Security

- The deploy hook URL contains a secret key. Treat it as a credential.
- Store only in GitHub Secrets, never in workflow YAML or tracked files.
- The `-fsS` curl flags suppress verbose output so the URL is not logged in GitHub Actions output.
- Render deploy hooks cannot be used to read service state, only to trigger deploys — low blast radius if leaked, but should still be rotated if exposed.
