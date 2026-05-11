# Deployment Notes: UI Modernization (Phase 8)

Feature: 003-ui-modernization  
Target: Render deployment for frontend and backend services

## Environment Variables

No new required environment variables were introduced for this feature.

Existing auth/API variables remain unchanged.

## Build and Runtime Impact

- Frontend includes modernized shared components and tokenized styling.
- Scanner feature now lazy-loads `@zxing/library` to reduce initial load cost.
- No backend API contract changes were introduced by UI modernization.

## Deployment Steps

1. Deploy backend service first (no schema changes required for this phase).
2. Deploy frontend service with latest static build.
3. In Render, verify frontend routing fallback remains configured for SPA paths.
4. Run smoke verification on:
   - `/login`
   - `/signup`
   - `/verify-email`
   - Authenticated child routes (`/reading-list`, `/search`, `/history`, `/rewards`)
   - Authenticated parent routes (`/parent`, `/parent/summary`)

## Post-Deploy Validation

1. Confirm accessibility regression status from `specs/003-ui-modernization/phase8-axe-results.json`.
2. Confirm Lighthouse baseline reports are archived:
   - `specs/003-ui-modernization/lighthouse-login.json`
   - `specs/003-ui-modernization/lighthouse-signup.json`
   - `specs/003-ui-modernization/lighthouse-verify-email.json`
3. Confirm visual baseline snapshots exist at:
   - `frontend/test-results/visual-regression/phase8-baseline`

## Known Follow-ups

- No additional Phase 8 blockers remain after the final VoiceOver smoke pass, Playwright rerun, and root lint command verification.
- Continue using the post-deploy smoke checklist above when promoting new frontend or backend images.
