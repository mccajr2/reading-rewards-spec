-- Add reading progress tracking mode support to book_reads
-- Supports PER_BOOK, CHAPTERS (per chapter), and PAGES (per page milestone) tracking

ALTER TABLE book_reads ADD COLUMN IF NOT EXISTS tracking_mode VARCHAR(20) DEFAULT 'BOOK_ONLY';
ALTER TABLE book_reads ADD COLUMN IF NOT EXISTS chapter_count_planned INTEGER;
ALTER TABLE book_reads ADD COLUMN IF NOT EXISTS chapters_completed INTEGER DEFAULT 0;
ALTER TABLE book_reads ADD COLUMN IF NOT EXISTS current_page INTEGER;
ALTER TABLE book_reads ADD COLUMN IF NOT EXISTS page_milestone_carry_forward INTEGER DEFAULT 0;

-- Index for efficient queries on tracking mode
CREATE INDEX IF NOT EXISTS idx_book_reads_tracking_mode ON book_reads(tracking_mode);
