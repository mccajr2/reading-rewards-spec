-- Link existing book_reads rows with reward selection and progress tracking entities.
-- Full constraints and foreign keys are added in later implementation tasks.

ALTER TABLE IF EXISTS book_reads
    ADD COLUMN IF NOT EXISTS reward_selection_id UUID;

ALTER TABLE IF EXISTS book_reads
    ADD COLUMN IF NOT EXISTS progress_tracking_id UUID;