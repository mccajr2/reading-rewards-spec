# Quickstart: Reading Rewards Parity Rebuild

## Current bootstrap state

- This repository is initialized with Git and GitHub Spec Kit.
- Spec Kit is installed locally in `.venv` using Python 3.12.
- The legacy behavior source remains `/Users/jasonmccarthy/projects/reading-rewards`.
- Backend and frontend parity workflows are implemented and runnable via Docker Compose.

## Current verification workflow

1. Submit parent signup at `/signup`.
2. Open the email verification link (`/verify-email?token=...`).
3. Verify backend status flips from `UNVERIFIED` to `VERIFIED`.
4. Log in at `/login` with the verified parent account.

## Parent dashboard and reversal verification workflow

1. Log in as a parent and navigate to `/parent/summary`.
2. Confirm the per-child summary cards show: child name, books in progress, completed count, and current balance.
3. Select a child card and confirm drill-down to `/parent/summary/:childId`.
4. In child detail, verify books, chapter read states, and reward history are visible.
5. Trigger a `Reverse` action on a read chapter and confirm the chapter switches to `Not read`.
6. Confirm the associated reward entry is removed and totals/balance refresh accordingly.

## Local tool requirements

- Java 21 runtime for the backend.
- Node.js 20 or newer.
- Python 3.12 for the repo-local Spec Kit tooling.
- Docker for PostgreSQL and end-to-end environment setup.

## Run the full stack

```bash
cd /Users/jasonmccarthy/projects/reading-rewards-spec
source .env.sh
docker-compose up -d
```

## Run tests

```bash
cd /Users/jasonmccarthy/projects/reading-rewards-spec/backend
JAVA_HOME=/opt/homebrew/opt/openjdk ./mvnw test

cd /Users/jasonmccarthy/projects/reading-rewards-spec/frontend
npm test -- --run

cd /Users/jasonmccarthy/projects/reading-rewards-spec
npx --yes playwright test --reporter=list
```

## Test evidence capture template

For each test run (backend, frontend, E2E), record the following in this section:

- Command: exact command executed
- Timestamp: local run time
- Exit code: `0` for pass, non-zero for fail
- Totals: passed/failed/skipped counts
- Failure details: failing test names and first actionable error (if any)

Example entry format:

```text
[backend] 2026-05-08 20:15 local
Command: cd backend && JAVA_HOME=/opt/homebrew/opt/openjdk ./mvnw test
Exit code: 0
Totals: 16 passed, 0 failed, 0 skipped

[frontend] 2026-05-08 20:17 local
Command: cd frontend && npm test -- --run
Exit code: 0
Totals: 18 passed, 0 failed

[e2e] 2026-05-08 20:20 local
Command: npx --yes playwright test --reporter=list
Exit code: 0
Totals: 11 passed, 0 failed
```

## Production email (Brevo)

In development and test environments, email verification is bypassed via the `DevAuthController` dev-only endpoint (`POST /api/auth/dev/verify?email=...`), which is disabled in the `prod` Spring profile.

To activate real email delivery in production:

1. Create a free [Brevo](https://brevo.com) account and obtain an API key.
2. Configure a verified sender identity in the Brevo dashboard.
3. Add the following to your production environment (e.g., `.env.sh` or your deployment secrets):
   ```
   BREVO_API_KEY=your-brevo-api-key
   BREVO_SENDER_EMAIL=noreply@yourdomain.com
   BREVO_SENDER_NAME=Reading Rewards
   ```
4. Deploy with `SPRING_PROFILES_ACTIVE=prod` to activate `BrevoEmailService` and disable `DevAuthController`.

## Spec Kit commands

- `./.venv/bin/specify check`
- `./.venv/bin/specify version`

## Repository goals

- Keep the old app unchanged.
- Keep all new work isolated to this repository.
- Preserve product behavior while improving structure and tests.