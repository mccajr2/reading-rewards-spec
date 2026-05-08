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
```

## Spec Kit commands

- `./.venv/bin/specify check`
- `./.venv/bin/specify version`

## Repository goals

- Keep the old app unchanged.
- Keep all new work isolated to this repository.
- Preserve product behavior while improving structure and tests.