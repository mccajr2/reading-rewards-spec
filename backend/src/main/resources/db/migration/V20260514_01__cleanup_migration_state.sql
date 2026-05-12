-- V20260514_01__cleanup_migration_state.sql
-- Idempotent cleanup: ensures schema is in valid baseline state
-- This migration validates baseline schema from V1__init.sql is present

-- Verify baseline tables exist (V1 tables)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='users') THEN
    RAISE EXCEPTION 'Schema validation failed: users table missing from V1__init.sql baseline';
  END IF;
  
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='books') THEN
    RAISE EXCEPTION 'Schema validation failed: books table missing from V1__init.sql baseline';
  END IF;
  
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='rewards') THEN
    RAISE EXCEPTION 'Schema validation failed: rewards table missing from V1__init.sql baseline';
  END IF;
END $$;

-- All baseline tables exist; schema is valid
-- No operational changes; this migration exists to:
-- 1. Mark a clean state transition from revert migration versions
-- 2. Provide explicit checkpoint for baseline schema validation
-- 3. Allow Flyway to recognize clean schema state
