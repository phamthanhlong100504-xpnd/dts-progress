-- V6__default_user_question_progress_is_critical.sql
-- Ensure is_critical has a default value so inserts omitting it do not violate not-null constraints.

ALTER TABLE user_question_progress
    ALTER COLUMN is_critical SET DEFAULT FALSE;
