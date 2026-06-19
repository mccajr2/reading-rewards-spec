-- Add per-book earning basis selection fields to book_reads

ALTER TABLE book_reads ADD COLUMN book_earning_basis VARCHAR(30);
ALTER TABLE book_reads ADD COLUMN basis_locked_at TIMESTAMP;
