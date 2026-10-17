package com.dts.progress.service;

import com.dts.progress.dto.event.PracticeExamFinishedEvent;
import com.dts.progress.dto.response.SessionHistoryResponse;
import com.dts.progress.entity.StudySession;
import com.dts.progress.exception.BusinessException;
import com.dts.progress.mapper.ProgressMapper;
import com.dts.progress.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CanonicalExamProgressService {

    private final JdbcTemplate jdbcTemplate;
    private final StudySessionRepository studySessionRepository;
    private final ProgressMapper mapper;

    @Transactional
    public SessionHistoryResponse apply(
            PracticeExamFinishedEvent event
    ) {
        validate(event);
        UUID userId = event.userId();

        jdbcTemplate.update("""
                INSERT INTO user_progress (user_id, username)
                VALUES (?, 'unknown')
                ON CONFLICT (user_id) DO NOTHING
                """,
                userId
        );

        // Tuần tự hóa POST từ FE cũ và event từ worker cho cùng user.
        jdbcTemplate.queryForObject("""
                SELECT id
                FROM user_progress
                WHERE user_id = ?
                FOR UPDATE
                """,
                UUID.class,
                userId
        );

        StudySession previous = studySessionRepository
                .findByUserIdAndExamId(userId, event.examId())
                .orElse(null);

        int oldQuestions = contribution(
                previous,
                previous == null
                        ? null
                        : previous.getQuestionsCount()
        );
        int oldCorrect = contribution(
                previous,
                previous == null
                        ? null
                        : previous.getCorrectCount()
        );
        int oldTime = contribution(
                previous,
                previous == null
                        ? null
                        : previous.getDurationSeconds()
        );
        int oldExams = contribution(
                previous,
                previous != null
                        && "EXAM".equals(previous.getSessionType())
                        ? 1 : 0
        );
        int oldPractice = contribution(
                previous,
                previous != null
                        && "PRACTICE".equals(previous.getSessionType())
                        ? 1 : 0
        );

        if (previous != null && sameValues(previous, event)) {
            return mapper.toSessionHistoryResponse(previous);
        }

        StudySession session = previous == null
                ? new StudySession()
                : previous;

        session.setUserId(userId);
        session.setExamId(event.examId());
        session.setSessionType(event.mode());
        session.setExamType(event.licenseClass());
        session.setMode(event.mode());
        session.setStatus(event.status());
        session.setQuestionsCount(event.totalQuestions());
        session.setCorrectCount(event.correctCount());
        session.setWrongCount(event.wrongCountLegacy());
        session.setScore(BigDecimal.valueOf(event.score()));
        session.setPassed(event.passed());
        session.setDurationSeconds(event.durationSeconds());
        session.setStartedAt(event.startedAt());
        session.setCompletedAt(event.completedAt());
        session.setUpdatedAt(Instant.now());

        session = studySessionRepository.saveAndFlush(session);

        jdbcTemplate.update("""
                UPDATE user_progress
                SET
                    total_questions_answered =
                        GREATEST(0, total_questions_answered + ?),
                    total_correct_answers =
                        GREATEST(0, total_correct_answers + ?),
                    total_study_time_seconds =
                        GREATEST(0, total_study_time_seconds + ?),
                    total_exams =
                        GREATEST(0, total_exams + ?),
                    total_practice_sessions =
                        GREATEST(0, total_practice_sessions + ?),
                    average_score = (
                        SELECT AVG(score)
                        FROM study_sessions
                        WHERE user_id = ?
                          AND status IN ('COMPLETED', 'TIMEOUT')
                          AND session_type IN ('EXAM', 'PRACTICE')
                    ),
                    updated_at = NOW(),
                    version = version + 1
                WHERE user_id = ?
                """,
                event.totalQuestions() - oldQuestions,
                event.correctCount() - oldCorrect,
                event.durationSeconds() - oldTime,
                ("EXAM".equals(event.mode()) ? 1 : 0) - oldExams,
                ("PRACTICE".equals(event.mode()) ? 1 : 0)
                        - oldPractice,
                userId,
                userId
        );

        updateStreak(userId);
        return mapper.toSessionHistoryResponse(session);
    }

    private void updateStreak(UUID userId) {
        List<LocalDate> dates = jdbcTemplate.query("""
                SELECT DISTINCT
                    (completed_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
                        AS study_date
                FROM study_sessions
                WHERE user_id = ?
                  AND completed_at IS NOT NULL
                  AND status IN ('COMPLETED', 'TIMEOUT')
                ORDER BY study_date
                """,
                (rs, row) -> rs.getObject(
                        "study_date",
                        LocalDate.class
                ),
                userId
        );

        int current = 0;
        int longest = 0;
        LocalDate previous = null;

        for (LocalDate date : dates) {
            current = previous != null
                    && previous.plusDays(1).equals(date)
                    ? current + 1
                    : 1;

            longest = Math.max(longest, current);
            previous = date;
        }

        jdbcTemplate.update("""
                UPDATE user_progress
                SET
                    last_study_date = ?,
                    current_streak = ?,
                    longest_streak =
                        GREATEST(longest_streak, ?),
                    updated_at = NOW(),
                    version = version + 1
                WHERE user_id = ?
                """,
                previous,
                current,
                longest,
                userId
        );
    }

    private int contribution(
            StudySession session,
            Integer value
    ) {
        return session != null
                && (
                        "COMPLETED".equals(session.getStatus())
                                || "TIMEOUT".equals(session.getStatus())
                )
                ? Objects.requireNonNullElse(value, 0)
                : 0;
    }

    private boolean sameValues(
            StudySession session,
            PracticeExamFinishedEvent event
    ) {
        return Objects.equals(
                        session.getSessionType(),
                        event.mode()
                )
                && Objects.equals(
                        session.getExamType(),
                        event.licenseClass()
                )
                && Objects.equals(
                        session.getMode(),
                        event.mode()
                )
                && Objects.equals(
                        session.getStatus(),
                        event.status()
                )
                && Objects.equals(
                        session.getQuestionsCount(),
                        event.totalQuestions()
                )
                && Objects.equals(
                        session.getCorrectCount(),
                        event.correctCount()
                )
                && Objects.equals(
                        session.getWrongCount(),
                        event.wrongCountLegacy()
                )
                && session.getScore() != null
                && session.getScore().compareTo(
                        BigDecimal.valueOf(event.score())
                ) == 0
                && Objects.equals(
                        session.getPassed(),
                        event.passed()
                )
                && Objects.equals(
                        session.getDurationSeconds(),
                        event.durationSeconds()
                )
                && Objects.equals(
                        session.getStartedAt(),
                        event.startedAt()
                )
                && Objects.equals(
                        session.getCompletedAt(),
                        event.completedAt()
                );
    }

    private void validate(
            PracticeExamFinishedEvent event
    ) {
        if (event == null
                || event.userId() == null
                || event.examId() == null
                || !event.examId().equals(event.eventId())
                || !"EXAM_FINISHED".equals(event.eventType())
                || !Integer.valueOf(1)
                        .equals(event.schemaVersion())
                || !"dts-practice".equals(event.source())
                || event.licenseClass() == null
                || event.licenseClass().isBlank()
                || (
                        event.bankVersion() != null
                                && event.bankVersion().isBlank()
                )
                || !(
                        "EXAM".equals(event.mode())
                                || "PRACTICE".equals(event.mode())
                )
                || !(
                        "COMPLETED".equals(event.status())
                                || "TIMEOUT".equals(event.status())
                )
                || event.totalQuestions() == null
                || event.totalQuestions() <= 0
                || event.correctCount() == null
                || event.correctCount() < 0
                || event.wrongCountLegacy() == null
                || event.wrongCountLegacy() < 0
                || event.answeredWrongCount() == null
                || event.answeredWrongCount() < 0
                || event.unansweredCount() == null
                || event.unansweredCount() < 0
                || event.score() == null
                || event.score() < 0
                || event.score() > 100
                || event.passed() == null
                || event.durationSeconds() == null
                || event.durationSeconds() < 0
                || event.startedAt() == null
                || event.completedAt() == null
                || event.completedAt()
                        .isBefore(event.startedAt())
                || (long) event.correctCount()
                        + event.answeredWrongCount()
                        + event.unansweredCount()
                        != event.totalQuestions()
                || (long) event.correctCount()
                        + event.wrongCountLegacy()
                        != event.totalQuestions()) {
            throw BusinessException.badRequest(
                    "Invalid canonical Practice exam progress"
            );
        }
    }
}