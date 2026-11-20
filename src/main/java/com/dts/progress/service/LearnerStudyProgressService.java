package com.dts.progress.service;

import com.dts.progress.dto.response.LearnerProgressModels.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LearnerStudyProgressService {

    private final JdbcTemplate jdbc;
    private static final String DEFAULT_BANK = "dts-2026-600-v1";

    @Transactional(readOnly = true)
    public List<LearnedAnswer> getChapterAnswers(UUID userId, String licenseClass, int chapterId) {
        String license = normalize(licenseClass);
        return jdbc.query("""
            SELECT question_id, COALESCE(selected_answer, ''), COALESCE(is_correct, false),
                   last_studied_at, last_answered_at
            FROM user_question_progress
            WHERE user_id = ? AND license_class = ? AND chapter_id = ? AND selected_answer IS NOT NULL
            ORDER BY question_id ASC
        """, (rs, rowNum) -> new LearnedAnswer(
            rs.getInt(1),
            rs.getString(2),
            rs.getBoolean(3),
            formatTs(rs.getObject(4, OffsetDateTime.class)),
            formatTs(rs.getObject(5, OffsetDateTime.class))
        ), userId, license, chapterId);
    }

    @Transactional(readOnly = true)
    public LearningCursor getChapterCursor(UUID userId, String licenseClass, int chapterId) {
        String license = normalize(licenseClass);
        List<LearningCursor> list = jdbc.query("""
            SELECT chapter_id, last_question_id, last_studied_at
            FROM learning_cursors
            WHERE user_id = ? AND license_class = ? AND chapter_id = ?
        """, (rs, rowNum) -> {
            int qId = rs.getInt(2);
            return new LearningCursor(
                rs.getInt(1),
                qId > 0 ? qId : null,
                qId > 0 ? qId : null,
                formatTs(rs.getObject(3, OffsetDateTime.class))
            );
        }, userId, license, chapterId);

        return list.isEmpty() ? new LearningCursor(chapterId, null, null, null) : list.get(0);
    }

    @Transactional
    public LearningCursor saveChapterCursor(UUID userId, String licenseClass, int chapterId, int questionId) {
        String license = normalize(licenseClass);
        OffsetDateTime now = OffsetDateTime.now();

        jdbc.update("""
            INSERT INTO learning_cursors (user_id, license_class, bank_version, chapter_id, last_question_id, last_studied_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (user_id, license_class, bank_version, chapter_id) DO UPDATE SET
                last_question_id = EXCLUDED.last_question_id,
                last_studied_at = EXCLUDED.last_studied_at
        """, userId, license, DEFAULT_BANK, chapterId, questionId, now);

        return new LearningCursor(chapterId, questionId, questionId, formatTs(now));
    }

    @Transactional(readOnly = true)
    public Bookmarks getChapterBookmarks(UUID userId, String licenseClass, int chapterId) {
        String license = normalize(licenseClass);
        List<Integer> ids = jdbc.queryForList("""
            SELECT question_id
            FROM user_question_progress
            WHERE user_id = ? AND license_class = ? AND chapter_id = ? AND is_bookmarked IS TRUE
            ORDER BY question_id ASC
        """, Integer.class, userId, license, chapterId);

        return new Bookmarks(chapterId, license, ids);
    }

    @Transactional
    public BookmarkResponse setBookmark(UUID userId, String licenseClass, int chapterId, int questionId, boolean bookmarked) {
        String license = normalize(licenseClass);
        OffsetDateTime now = OffsetDateTime.now();

        jdbc.update("""
            INSERT INTO user_question_progress (
                user_id, license_class, bank_version, question_id, chapter_id,
                is_bookmarked, bookmarked_at, last_studied_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (user_id, license_class, bank_version, question_id) DO UPDATE SET
                is_bookmarked = EXCLUDED.is_bookmarked,
                bookmarked_at = CASE WHEN EXCLUDED.is_bookmarked THEN EXCLUDED.bookmarked_at ELSE user_question_progress.bookmarked_at END
        """, userId, license, DEFAULT_BANK, questionId, chapterId, bookmarked, now, now);

        return new BookmarkResponse(chapterId, questionId, license, bookmarked);
    }

    @Transactional(readOnly = true)
    public CriticalOverview getCriticalOverview(UUID userId, String licenseClass) {
        String license = normalize(licenseClass);
        List<CriticalItem> questions = jdbc.query("""
            SELECT question_id, chapter_id, COALESCE(critical_learned, false),
                   selected_answer, is_correct, last_studied_at
            FROM user_question_progress
            WHERE user_id = ? AND license_class = ? AND is_critical IS TRUE
            ORDER BY question_id ASC
        """, (rs, rowNum) -> new CriticalItem(
            rs.getInt(1),
            rs.getInt(2),
            rs.getBoolean(3),
            rs.getString(4),
            rs.getObject(5) != null ? rs.getBoolean(5) : null,
            formatTs(rs.getObject(6, OffsetDateTime.class))
        ), userId, license);

        int total = questions.size();
        int learned = (int) questions.stream().filter(CriticalItem::learned).count();
        return new CriticalOverview(total, learned, total - learned, questions);
    }

    @Transactional(readOnly = true)
    public WrongOverview getWrongOverview(UUID userId, String licenseClass) {
        String license = normalize(licenseClass);
        List<WrongItem> questions = jdbc.query("""
            SELECT question_id, chapter_id, COALESCE(is_critical, false),
                   COALESCE(last_wrong_answer, ''), last_wrong_at, last_answered_at
            FROM user_question_progress
            WHERE user_id = ? AND license_class = ? AND needs_review IS TRUE
            ORDER BY last_wrong_at DESC NULLS LAST
        """, (rs, rowNum) -> new WrongItem(
            rs.getInt(1),
            rs.getInt(2),
            rs.getBoolean(3),
            rs.getString(4),
            formatTs(rs.getObject(5, OffsetDateTime.class)),
            formatTs(rs.getObject(6, OffsetDateTime.class))
        ), userId, license);

        return new WrongOverview(questions.size(), questions);
    }

    private static String normalize(String licenseClass) {
        return licenseClass == null ? "B2" : licenseClass.trim().toUpperCase(Locale.ROOT);
    }

    private static String formatTs(OffsetDateTime ts) {
        return ts == null ? null : ts.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
