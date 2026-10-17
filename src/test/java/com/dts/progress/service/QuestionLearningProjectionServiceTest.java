package com.dts.progress.service;

import com.dts.progress.dto.event.PracticeQuestionProgressEvent;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class QuestionLearningProjectionServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("dts_progress")
                    .withUsername("postgres")
                    .withPassword("postgres");

    static JdbcTemplate jdbc;
    static QuestionLearningProjectionService service;

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(),
                        postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load().migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword()));
        service = new QuestionLearningProjectionService(jdbc);
    }

    @Test
    void repeatedStudyAndReplayCountOneDistinctQuestion() {
        UUID user = UUID.randomUUID();
        PracticeQuestionProgressEvent first = event(
                user, "A1", 601, 101L, "CHAPTER", true, null);
        service.apply(first);
        service.apply(first);
        service.apply(event(user, "A1", 601, 102L, "CHAPTER", true, null));

        assertEquals(1, count("""
                SELECT count(*) FROM user_question_progress
                WHERE user_id = ? AND license_class = 'A1'
                  AND chapter_learned
                """, user));
        assertEquals(2, count("""
                SELECT count(*) FROM practice_learning_event_inbox
                WHERE user_id = ?
                """, user));
    }

    @Test
    void oldWrongEventCannotUndoNewCorrectAnswer() {
        UUID user = UUID.randomUUID();
        service.apply(event(user, "A1", 601, 202L, "REVIEW", null, false));
        service.apply(event(user, "A1", 601, 201L, "REVIEW", null, true));
        assertEquals(false, jdbc.queryForObject("""
                SELECT needs_review FROM user_question_progress
                WHERE user_id = ? AND license_class = 'A1' AND question_id = 601
                """, Boolean.class, user));
    }

    @Test
    void sameQuestionIsIndependentAcrossUserLicenseAndBank() {
        UUID firstUser = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        service.apply(event(firstUser, "A1", 601, 301L, "CHAPTER", true, null));
        service.apply(event(firstUser, "C1", 601, 302L, "CHAPTER", true, null));
        service.apply(event(otherUser, "A1", 601, 303L, "CHAPTER", true, null));

        PracticeQuestionProgressEvent newBank = event(
                firstUser, "A1", 601, 304L, "CHAPTER", true, null);
        service.apply(new PracticeQuestionProgressEvent(
                newBank.eventId(), newBank.eventType(), newBank.schemaVersion(),
                newBank.source(), newBank.eventSequence(), newBank.userId(),
                newBank.licenseClass(), "dts-2027-v1", newBank.questionId(),
                newBank.chapterId(), newBank.critical(), newBank.dimension(),
                newBank.learned(), newBank.needsReview(), newBank.studiedAt()));

        assertEquals(4, count("""
                SELECT count(*) FROM user_question_progress
                WHERE question_id = 601
                  AND user_id IN (?, ?)
                """, firstUser, otherUser));
        assertEquals(1, count("""
                SELECT count(*) FROM user_question_progress
                WHERE user_id = ? AND license_class = 'A1'
                  AND bank_version = 'dts-2026-600-v1'
                """, firstUser));
    }

    @Test
    void criticalStudyAddsOneEvenAfterChapterStudy() {
        UUID user = UUID.randomUUID();
        service.apply(event(user, "A1", 601, 401L, "CHAPTER", true, null));
        service.apply(event(user, "A1", 601, 402L, "CRITICAL", true, null));
        assertEquals(1, count("""
                SELECT count(*) FROM user_question_progress
                WHERE user_id = ? AND is_critical
                  AND (chapter_learned OR critical_learned)
                """, user));
    }

    private static PracticeQuestionProgressEvent event(
            UUID userId, String license, int questionId, long sequence,
            String dimension, Boolean learned, Boolean needsReview
    ) {
        return new PracticeQuestionProgressEvent(
                UUID.randomUUID(), "QUESTION_PROGRESS_CHANGED", 1,
                "dts-practice", sequence, userId, license,
                "dts-2026-600-v1", questionId, 1, true,
                dimension, learned, needsReview, Instant.now());
    }

    private static long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }
}
