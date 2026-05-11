# Contract: Backend Health Check Endpoint

**Feature**: `002-deployment-cicd` | **Date**: 2026-05-10

## Overview

The `/actuator/health` endpoint is the Render health check target for the backend service. Render calls this endpoint after deployment to determine whether the service is healthy before marking the deploy as successful.

---

## Endpoint

```
GET /actuator/health
```

**Base URL**: `https://reading-rewards-spec-backend.onrender.com` (production)

---

## Response: Healthy

**HTTP Status**: `200 OK`  
**Content-Type**: `application/json`

```json
{
  "status": "UP"
}
```

Or with detailed components (if `management.endpoint.health.show-details` is enabled):

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

---

## Response: Unhealthy

**HTTP Status**: `503 Service Unavailable`

```json
{
  "status": "DOWN",
  "components": {
    "db": {
      "status": "DOWN",
      "details": {
        "error": "..."
      }
    }
  }
}
```

Render treats any non-2xx response as a health check failure and will not mark the deploy as successful.

---

## Configuration

Enabled in `backend/src/main/resources/application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      probes:
        enabled: true
```

No authentication is required for `/actuator/health`. Spring Boot Actuator's default security permits unauthenticated health checks.

---

## Render Health Check Configuration

In `render.yaml`:

```yaml
healthCheckPath: /actuator/health
```

Render checks this path after container start. If the endpoint does not return HTTP 200 within the configured timeout, the deploy is marked as failed and the previous version continues serving traffic.
