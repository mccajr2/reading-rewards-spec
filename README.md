# Reading Rewards Spec

This repository is the greenfield successor to the existing Reading Rewards app.

## Current Status

Implemented so far:

- Spec Kit initialized with project constitution and first parity feature artifacts.
- Backend auth parity foundation implemented:
  - parent signup and verification endpoints
  - parent verification gate at login
  - child username login path
  - JWT-based stateless auth filter and protected endpoint check
- Frontend auth parity foundation implemented:
  - persisted auth context using local storage
  - route gating for unauthenticated and authenticated states
  - login and signup pages in plain React and CSS
- Automated tests in place for backend and frontend foundations.

## Repositories

Legacy baseline repository:

- /Users/jasonmccarthy/projects/reading-rewards

Successor repository:

- /Users/jasonmccarthy/projects/reading-rewards-spec

## Toolchains

- Java: 25
- Node.js: 20+
- Python: 3.12 (for local Spec Kit CLI in .venv)

## Book Search Provider

- The backend book search integration uses Open Library via `backend/src/main/java/com/example/readingrewards/domain/service/GoogleBooksService.java`.
- Open Library is a free external dependency (no API key required for current usage).
- Fallback behavior: if Open Library is unavailable or returns malformed data, the API returns an empty list rather than failing the request.

## Local Setup

### 1. Backend

From backend folder:

```bash
cd backend
mvn test
mvn spring-boot:run
```

### 2. Frontend

From frontend folder:

```bash
cd frontend
npm install
npm test -- --run
npm run build
npm run dev
```

### 3. Spec Kit

From repository root:

```bash
python3.12 -m venv .venv
.venv/bin/pip install --upgrade pip
.venv/bin/pip install git+https://github.com/github/spec-kit.git@v0.8.7
.venv/bin/specify check
```

## Spec Artifacts

Primary feature artifacts are under:

- specs/001-reading-rewards-parity/

Key files:

- specs/001-reading-rewards-parity/spec.md
- specs/001-reading-rewards-parity/plan.md
- specs/001-reading-rewards-parity/research.md
- specs/001-reading-rewards-parity/data-model.md

## Next Milestones

1. Implement parent child-management API parity in backend.
2. Implement reading-progress and rewards domain parity.
3. Wire frontend pages for search, reading list, history, rewards, and parent summary.
4. Add end-to-end smoke tests for parent and child critical journeys.
