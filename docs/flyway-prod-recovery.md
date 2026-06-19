# Flyway Production Recovery (Neon)

If the backend fails on Render with:

```
Validate failed: Detected resolved migration not applied to database: 2..7
```

the Neon database was likely deployed with only `V1__init.sql` while newer migrations
(`V2`–`V7`) were added later in the repo.

## Expected fix (automatic)

`application-prod.yml` configures Flyway to:

- apply pending migrations on startup (`validate-on-migrate: false` during recovery)
- allow out-of-order gap filling when needed
- ignore pending/ignored scripts during validation (`ProdFlywayConfiguration` + YAML patterns)

Redeploy the backend **from an image that includes this change** (merge to `main` and let CI push GHCR, or trigger Render manually after merge).

## Verify on Neon

In the Neon SQL editor, inspect migration history:

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

After a successful deploy you should see versions `1` through `7`.

## Manual recovery (only if deploy still fails)

1. Confirm which tables already exist (for example `reward_options`, `reward_settlement_requests`).
2. If schema objects exist but Flyway history is missing versions, either:
   - drop the orphaned objects and redeploy, or
   - insert matching rows into `flyway_schema_history` for migrations already reflected in the schema (advanced; prefer redeploy on a fresh branch DB for non-prod).

For production, prefer fixing Flyway config and redeploying before manual history edits.
