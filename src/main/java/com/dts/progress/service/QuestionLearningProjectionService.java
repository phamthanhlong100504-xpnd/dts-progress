package com.dts.progress.service;

import com.dts.progress.dto.event.PracticeQuestionProgressEvent;
import com.dts.progress.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class QuestionLearningProjectionService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void apply(PracticeQuestionProgressEvent event) {
        validate(event);

        int inserted = jdbcTemplate.update("""
                INSERT INTO practice_learning_event_inbox
                    (event_id, event_sequence, user_id, license_class,
                     bank_version, question_id)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (event_id) DO NOTHING
                """,
                event.eventId(), event.eventSequence(), event.userId(),
                event.licenseClass(), event.bankVersion(), event.questionId());

        if (inserted == 0) {
            return;
        }

        boolean chapter = "CHAPTER".equals(event.dimension());
        boolean critical = "CRITICAL".equals(event.dimension());
        boolean review = "REVIEW".equals(event.dimension());

        OffsetDateTime studiedAt =
                event.studiedAt().atOffset(ZoneOffset.UTC);

        jdbcTemplate.update("""
                INSERT INTO user_question_progress (
                    user_id, license_class, bank_version, question_id,
                    chapter_id, is_critical, chapter_learned, critical_learned,
                    needs_review, chapter_version, critical_version,
                    review_version, last_studied_at, last_reviewed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, license_class, bank_version, question_id)
                DO UPDATE SET
                    chapter_learned = CASE
                        WHEN EXCLUDED.chapter_version > user_question_progress.chapter_version
                        THEN EXCLUDED.chapter_learned
                        ELSE user_question_progress.chapter_learned END,
                    critical_learned = CASE
                        WHEN EXCLUDED.critical_version > user_question_progress.critical_version
                        THEN EXCLUDED.critical_learned
                        ELSE user_question_progress.critical_learned END,
                    needs_review = CASE
                        WHEN EXCLUDED.review_version > user_question_progress.review_version
                        THEN EXCLUDED.needs_review
                        ELSE user_question_progress.needs_review END,
                    last_studied_at = CASE
                        WHEN EXCLUDED.chapter_version > user_question_progress.chapter_version
                          OR EXCLUDED.critical_version > user_question_progress.critical_version
                        THEN GREATEST(user_question_progress.last_studied_at,
                                      EXCLUDED.last_studied_at)
                        ELSE user_question_progress.last_studied_at END,
                    last_reviewed_at = CASE
                        WHEN EXCLUDED.review_version > user_question_progress.review_version
                        THEN GREATEST(user_question_progress.last_reviewed_at,
                                      EXCLUDED.last_reviewed_at)
                        ELSE user_question_progress.last_reviewed_at END,
                    chapter_version = GREATEST(user_question_progress.chapter_version,
                                               EXCLUDED.chapter_version),
                    critical_version = GREATEST(user_question_progress.critical_version,
                                                EXCLUDED.critical_version),
                    review_version = GREATEST(user_question_progress.review_version,
                                              EXCLUDED.review_version),
                    chapter_id = EXCLUDED.chapter_id,
                    is_critical = EXCLUDED.is_critical
                """,
                event.userId(), event.licenseClass(), event.bankVersion(),
                event.questionId(), event.chapterId(), event.critical(),
                chapter && event.learned(),
                critical && event.learned(),
                review && event.needsReview(),
                chapter ? event.eventSequence() : 0L,
                critical ? event.eventSequence() : 0L,
                review ? event.eventSequence() : 0L,
                review ? null : studiedAt,
                review ? studiedAt : null
        );
    }

    private static void validate(PracticeQuestionProgressEvent event) {
        if (event == null || event.eventId() == null
                || !"QUESTION_PROGRESS_CHANGED".equals(event.eventType())
                || !Integer.valueOf(1).equals(event.schemaVersion())
                || !"dts-practice".equals(event.source())
                || event.eventSequence() == null || event.eventSequence() <= 0
                || event.userId() == null
                || event.licenseClass() == null || event.licenseClass().isBlank()
                || event.bankVersion() == null || event.bankVersion().isBlank()
                || event.questionId() == null || event.questionId() <= 0
                || event.chapterId() == null || event.chapterId() <= 0
                || event.critical() == null || event.studiedAt() == null
                || !("CHAPTER".equals(event.dimension())
                     || "CRITICAL".equals(event.dimension())
                     || "REVIEW".equals(event.dimension()))
                || ("REVIEW".equals(event.dimension())
                    ? event.needsReview() == null || event.learned() != null
                    : event.learned() == null || event.needsReview() != null)) {
            throw BusinessException.badRequest(
                    "Invalid Practice question progress event"
            );
        }
    }
}