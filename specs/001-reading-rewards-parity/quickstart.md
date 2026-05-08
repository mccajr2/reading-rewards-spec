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