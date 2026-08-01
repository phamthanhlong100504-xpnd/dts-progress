-- V1: Initialize Progress Service Schema
-- Tracks user learning progress, chapter completion, and study sessions

-- ==================== USER PROGRESS ====================
CREATE TABLE IF NOT EXISTS user_progress (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL UNIQUE,
    username        VARCHAR(100)    NOT NULL,

    -- Overall stats
    total_exams             INTEGER NOT NULL DEFAULT 0,
    total_practice_sessions INTEGER NOT NULL DEFAULT 0,
    total_study_time_seconds BIGINT NOT NULL DEFAULT 0,
    total_questions_answered INTEGER NOT NULL DEFAULT 0,
    total_correct_answers   INTEGER NOT NULL DEFAULT 0,
    average_score           NUMERIC(5,2),

    -- Streak tracking
    current_streak  INTEGER NOT NULL DEFAULT 0,
    longest_streak  INTEGER NOT NULL DEFAULT 0,
    last_study_date DATE,

    -- Audit
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT      NOT NULL DEFAULT 1
);

-- ==================== CHAPTER PROGRESS ====================
CREATE TABLE IF NOT EXISTS chapter_progress (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,
    chapter_id      INTEGER         NOT NULL,
    chapter_name    VARCHAR(200)    NOT NULL,

    -- Progress status
    status              VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',  -- NOT_STARTED, IN_PROGRESS, COMPLETED
    questions_answered  INTEGER     NOT NULL DEFAULT 0,
    questions_total     INTEGER     NOT NULL DEFAULT 0,
    correct_count       INTEGER     NOT NULL DEFAULT 0,
    score               NUMERIC(5,2),

    -- Timestamps
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    last_studied_at TIMESTAMPTZ,

    -- Audit
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT      NOT NULL DEFAULT 1,

    UNIQUE (user_id, chapter_id)
);

-- ==================== STUDY SESSION ====================
CREATE TABLE IF NOT EXISTS study_sessions (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,
    exam_id         UUID,           -- nullable: practice sessions may not have an exam

    -- Session details
    session_type    VARCHAR(20)     NOT NULL,  -- EXAM, PRACTICE
    exam_type       VARCHAR(10),               -- A1, A2, B1, B2, etc.
    mode            VARCHAR(10),               -- EXAM, PRACTICE
    status          VARCHAR(20)     NOT NULL DEFAULT 'IN_PROGRESS',  -- IN_PROGRESS, COMPLETED, TIMEOUT

    -- Results
    questions_count INTEGER,
    correct_count   INTEGER,
    wrong_count     INTEGER,
    score           NUMERIC(5,2),
    passed          BOOLEAN,

    -- Time tracking
    duration_seconds INTEGER,
    started_at      TIMESTAMPTZ NOT NULL,
    completed_at    TIMESTAMPTZ,

    -- Audit
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ==================== INDEXES ====================
CREATE INDEX idx_user_progress_user_id ON user_progress(user_id);
CREATE INDEX idx_chapter_progress_user_id ON chapter_progress(user_id);
CREATE INDEX idx_chapter_progress_user_chapter ON chapter_progress(user_id, chapter_id);
CREATE INDEX idx_study_sessions_user_id ON study_sessions(user_id);
CREATE INDEX idx_study_sessions_user_started ON study_sessions(user_id, started_at DESC);
CREATE INDEX idx_study_sessions_status ON study_sessions(status);
