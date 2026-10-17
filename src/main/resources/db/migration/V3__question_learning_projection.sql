-- Separate Screen 15 projection. Legacy dashboard and chapter_progress stay intact.
CREATE TABLE user_question_progress (
    user_id            UUID NOT NULL,
    license_class      VARCHAR(10) NOT NULL,
    bank_version       VARCHAR(64) NOT NULL,
    question_id        INTEGER NOT NULL,
    chapter_id         INTEGER NOT NULL CHECK (chapter_id > 0),
    is_critical        BOOLEAN NOT NULL,
    chapter_learned    BOOLEAN NOT NULL DEFAULT FALSE,
    critical_learned   BOOLEAN NOT NULL DEFAULT FALSE,
    needs_review       BOOLEAN NOT NULL DEFAULT FALSE,
    chapter_version    BIGINT NOT NULL DEFAULT 0,
    critical_version   BIGINT NOT NULL DEFAULT 0,
    review_version     BIGINT NOT NULL DEFAULT 0,
    last_studied_at    TIMESTAMPTZ,
    last_reviewed_at   TIMESTAMPTZ,
    PRIMARY KEY (user_id, license_class, bank_version, question_id)
);

CREATE INDEX idx_user_question_progress_overview
    ON user_question_progress (user_id, license_class, bank_version, chapter_id);

CREATE TABLE practice_learning_event_inbox (
    event_id          UUID PRIMARY KEY,
    event_sequence    BIGINT NOT NULL,
    user_id           UUID NOT NULL,
    license_class     VARCHAR(10) NOT NULL,
    bank_version      VARCHAR(64) NOT NULL,
    question_id       INTEGER NOT NULL,
    received_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_practice_learning_event_inbox_scope
    ON practice_learning_event_inbox (user_id, license_class, bank_version, question_id);
