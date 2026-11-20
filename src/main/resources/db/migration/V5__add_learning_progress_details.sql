-- V5__add_learning_progress_details.sql
-- Expand user_question_progress and add learning_cursors to fully own learning progress domain in dts-progress.

ALTER TABLE user_question_progress
    ADD COLUMN IF NOT EXISTS is_bookmarked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS bookmarked_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS selected_answer VARCHAR(10),
    ADD COLUMN IF NOT EXISTS is_correct BOOLEAN,
    ADD COLUMN IF NOT EXISTS last_answered_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_wrong_answer VARCHAR(10),
    ADD COLUMN IF NOT EXISTS last_wrong_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS learning_cursors (
    user_id UUID NOT NULL,
    license_class VARCHAR(10) NOT NULL,
    bank_version VARCHAR(64) NOT NULL,
    chapter_id INTEGER NOT NULL CHECK (chapter_id > 0),
    last_question_id INTEGER NOT NULL,
    last_studied_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, license_class, bank_version, chapter_id)
);

CREATE INDEX IF NOT EXISTS idx_uqp_user_license_bookmarked 
    ON user_question_progress (user_id, license_class, is_bookmarked) 
    WHERE is_bookmarked IS TRUE;

CREATE INDEX IF NOT EXISTS idx_uqp_user_license_wrong 
    ON user_question_progress (user_id, license_class, needs_review) 
    WHERE needs_review IS TRUE;

CREATE INDEX IF NOT EXISTS idx_uqp_user_license_critical 
    ON user_question_progress (user_id, license_class, is_critical);
