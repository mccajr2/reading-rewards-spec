# Reward Customization Release Notes

## Overview

Release package for feature `004-reward-customization` including parent reward management, child reward selection/progress, payout reminders, encouragement messaging, and mixed reward type display.

## Included Capabilities

1. Parent reward configuration
- Family-level templates
- Per-child overrides
- Canned templates
- Soft-delete (archive) behavior

2. Child reward workflow
- Reward selection per book with locked rate snapshot
- Progress tracking (chapter/page/none)
- Reward accumulation and balance/history views

3. Payout and messaging loop
- Child payout reminders (in-app + optional parent email)
- Parent payout confirmation
- Parent encouragement messages (in-app only)
- Child message inbox and read-state support

4. Mixed reward type rendering
- Aggregation and display for money, time, and custom reward types
- Child reward cards by type

## Deployment Notes

1. Database migrations
- Ensure reward customization migrations are applied before backend rollout.
- Verify message and accumulation tables are present and writable.

2. Backend
- Deploy Spring Boot service with reward and message controllers enabled.
- Verify authentication profile allows expected `/api/auth/dev/**` behavior only in non-prod.

3. Frontend
- Deploy Vite build with new parent and child rewards routes.
- Confirm role-based routing to `/parent/rewards` and `/child/rewards`.

## Rollout Checklist

- [ ] Run targeted backend checks
  - `mvn -f backend/pom.xml -q -DskipTests compile`
- [ ] Run targeted frontend checks
  - `npm --prefix frontend test -- src/features/rewards/RewardsPage.test.tsx`
- [ ] Run reward regression e2e flow
  - `npm run test:e2e -- tests/e2e/reward-customization.spec.ts`
- [ ] Run accessibility e2e checks
  - `npm run test:e2e -- tests/e2e/reward-accessibility.spec.ts`
- [ ] Generate Lighthouse baseline
  - `node scripts/phase8-rewards-performance-audit.mjs`
- [ ] Capture screenshots listed in quickstart checklist
- [ ] Validate parent and child login journeys in production-like environment
- [ ] Confirm payout and message flows for at least one test family

## Rollback Plan

1. Frontend rollback
- Revert to prior frontend bundle and clear CDN cache.

2. Backend rollback
- Revert service deployment to previous tag.
- Keep database schema intact (migrations are additive and backward-compatible for reads).

3. Data safety
- Reward and message records are append-safe; no destructive migration in this release.

## Post-Release Monitoring

- Track `/api/child/rewards/balance` and `/api/parent/rewards/*` error rates.
- Monitor message endpoints for read/write failures.
- Monitor average response time on child rewards pages.
- Review support tickets for payout discrepancies or route-access confusion.
