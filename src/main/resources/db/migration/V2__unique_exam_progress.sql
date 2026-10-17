CREATE UNIQUE INDEX uq_study_sessions_user_exam
    ON study_sessions (user_id, exam_id)
    WHERE exam_id IS NOT NULL;