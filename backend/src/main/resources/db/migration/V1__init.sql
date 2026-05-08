-- V1__init.sql
-- Initial schema for Reading Rewards

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role VARCHAR(10) NOT NULL CHECK (role IN ('PARENT', 'CHILD')),
    parent_id UUID,
    email VARCHAR(255) UNIQUE,
    username VARCHAR(100) UNIQUE,
    first_name VARCHAR(100),
    password VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
    verification_token VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_parent FOREIGN KEY (parent_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT email_required_for_parent CHECK ((role = 'PARENT' AND email IS NOT NULL) OR role = 'CHILD'),
    CONSTRAINT username_required_for_child CHECK ((role = 'CHILD' AND username IS NOT NULL) OR role = 'PARENT'),
    CONSTRAINT parent_required_for_child CHECK ((role = 'CHILD' AND parent_id IS NOT NULL) OR role = 'PARENT'),
    CONSTRAINT parent_cannot_have_parent CHECK ((role = 'PARENT' AND parent_id IS NULL) OR role = 'CHILD')
);

CREATE INDEX IF NOT EXISTS idx_users_parent_id ON users(parent_id);

CREATE TABLE IF NOT EXISTS books (
    google_book_id VARCHAR(50) PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    thumbnail_url VARCHAR(1000),
    authors TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chapters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    google_book_id VARCHAR(50) NOT NULL,
    name VARCHAR(500) NOT NULL,
    chapter_index INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chapter_book FOREIGN KEY (google_book_id) REFERENCES books(google_book_id) ON DELETE CASCADE,
    CONSTRAINT uq_book_chapter_index UNIQUE (google_book_id, chapter_index)
);

CREATE INDEX IF NOT EXISTS idx_chapters_google_book_id ON chapters(google_book_id);

CREATE TABLE IF NOT EXISTS book_reads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    google_book_id VARCHAR(50) NOT NULL,
    user_id UUID NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_book_read_book FOREIGN KEY (google_book_id) REFERENCES books(google_book_id) ON DELETE CASCADE,
    CONSTRAINT fk_book_read_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_book_reads_user_id ON book_reads(user_id);
CREATE INDEX IF NOT EXISTS idx_book_reads_google_book_id ON book_reads(google_book_id);

CREATE TABLE IF NOT EXISTS chapter_reads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_read_id UUID NOT NULL,
    chapter_id UUID NOT NULL,
    user_id UUID NOT NULL,
    completion_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chapter_read_book_read FOREIGN KEY (book_read_id) REFERENCES book_reads(id) ON DELETE CASCADE,
    CONSTRAINT fk_chapter_read_chapter FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE,
    CONSTRAINT fk_chapter_read_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_chapter_reads_book_read_id ON chapter_reads(book_read_id);
CREATE INDEX IF NOT EXISTS idx_chapter_reads_user_id ON chapter_reads(user_id);

CREATE TABLE IF NOT EXISTS rewards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(10) NOT NULL CHECK (type IN ('EARN', 'SPEND', 'PAYOUT')),
    user_id UUID NOT NULL,
    amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    chapter_read_id UUID,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reward_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reward_chapter_read FOREIGN KEY (chapter_read_id) REFERENCES chapter_reads(id) ON DELETE SET NULL,
    CONSTRAINT chapter_read_required_for_earn CHECK ((type = 'EARN' AND chapter_read_id IS NOT NULL) OR type != 'EARN')
);

CREATE INDEX IF NOT EXISTS idx_rewards_user_id ON rewards(user_id);
CREATE INDEX IF NOT EXISTS idx_rewards_type ON rewards(type);
