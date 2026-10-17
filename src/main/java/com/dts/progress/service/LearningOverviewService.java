package com.dts.progress.service;

import com.dts.progress.dto.response.LearningBankCatalogResponse;
import com.dts.progress.dto.response.LearningOverviewResponse;
import com.dts.progress.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearningOverviewService {

    private final JdbcTemplate jdbcTemplate;
    private final PracticeLearningBankClient catalogClient;

    @Transactional(readOnly = true)
    public LearningOverviewResponse get(
            UUID userId,
            String licenseClass,
            String authorization
    ) {
        if (userId == null || licenseClass == null || licenseClass.isBlank()) {
            throw BusinessException.badRequest("License class is required");
        }

        String license = licenseClass.trim().toUpperCase(Locale.ROOT);
        LearningBankCatalogResponse catalog =
                catalogClient.get(license, authorization);

        Map<Integer, Stats> stats = new HashMap<>();

        jdbcTemplate.query("""
                SELECT chapter_id,
                       COUNT(*) FILTER (
                           WHERE chapter_learned OR critical_learned
                       )::integer AS learned,
                       COUNT(*) FILTER (
                           WHERE needs_review
                       )::integer AS review,
                       COUNT(*) FILTER (
                           WHERE is_critical
                             AND (chapter_learned OR critical_learned)
                       )::integer AS critical,
                       MAX(last_studied_at) AS last_studied_at
                FROM user_question_progress
                WHERE user_id = ?
                  AND license_class = ?
                  AND bank_version = ?
                GROUP BY chapter_id
                """,
                rs -> {
                    Timestamp last = rs.getTimestamp("last_studied_at");

                    stats.put(
                            rs.getInt("chapter_id"),
                            new Stats(
                                    rs.getInt("learned"),
                                    rs.getInt("review"),
                                    rs.getInt("critical"),
                                    last == null ? null : last.toInstant()
                            )
                    );
                },
                userId,
                license,
                catalog.bankVersion()
        );

        List<LearningOverviewResponse.Chapter> chapters =
                catalog.chapters()
                        .stream()
                        .filter(chapter -> chapter.totalQuestions() > 0)
                        .map(chapter -> {
                            if (chapter.chapterName() == null
                                    || chapter.chapterName().isBlank()) {
                                throw BusinessException.conflict(
                                        "Practice catalog is missing chapter metadata"
                                );
                            }

                            Stats value = stats.getOrDefault(
                                    chapter.chapterId(),
                                    Stats.ZERO
                            );

                            if (value.learned() > chapter.totalQuestions()
                                    || value.critical() > chapter.criticalQuestions()
                                    || value.review() > chapter.totalQuestions()) {
                                throw BusinessException.conflict(
                                        "Question progress exceeds canonical question bank"
                                );
                            }

                            return new LearningOverviewResponse.Chapter(
                                    chapter.chapterId(),
                                    chapter.chapterName(),
                                    chapter.totalQuestions(),
                                    value.learned(),
                                    percent(
                                            value.learned(),
                                            chapter.totalQuestions()
                                    ),
                                    status(
                                            value.learned(),
                                            chapter.totalQuestions()
                                    ),
                                    value.review(),
                                    value.critical(),
                                    value.lastStudiedAt()
                            );
                        })
                        .toList();

        int learned = chapters.stream()
                .mapToInt(LearningOverviewResponse.Chapter::learnedQuestions)
                .sum();

        int review = chapters.stream()
                .mapToInt(LearningOverviewResponse.Chapter::needReviewCount)
                .sum();

        int critical = chapters.stream()
                .mapToInt(
                        LearningOverviewResponse.Chapter::criticalStudiedQuestions
                )
                .sum();

        return new LearningOverviewResponse(
                license,
                catalog.bankVersion(),
                new LearningOverviewResponse.Overall(
                        catalog.totalQuestions(),
                        learned,
                        percent(learned, catalog.totalQuestions()),
                        status(learned, catalog.totalQuestions())
                ),
                new LearningOverviewResponse.Review(review),
                new LearningOverviewResponse.Critical(
                        catalog.criticalQuestions(),
                        critical,
                        percent(critical, catalog.criticalQuestions()),
                        status(critical, catalog.criticalQuestions())
                ),
                chapters
        );
    }

    private static int percent(int count, int total) {
        return total == 0 ? 0 : Math.round(100f * count / total);
    }

    private static String status(int learned, int total) {
        if (learned == 0 || total == 0) {
            return "NOT_STARTED";
        }
        return learned == total ? "COMPLETED" : "IN_PROGRESS";
    }

    private record Stats(
            int learned,
            int review,
            int critical,
            Instant lastStudiedAt
    ) {
        private static final Stats ZERO = new Stats(0, 0, 0, null);
    }
}