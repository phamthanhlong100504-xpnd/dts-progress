package com.dts.progress.dto.response;

import java.time.Instant;
import java.util.List;

public record LearningOverviewResponse(
        String licenseClass,
        String bankVersion,
        Overall overall,
        Review review,
        Critical critical,
        List<Chapter> chapters
) {
    public record Overall(
            int totalQuestions,
            int learnedQuestions,
            int completionPercent,
            String status
    ) {}

    public record Review(int needReviewCount) {}

    public record Critical(
            int totalQuestions,
            int criticalStudiedQuestions,
            int completionPercent,
            String status
    ) {}

    public record Chapter(
            int chapterId,
            String chapterName,
            int totalQuestions,
            int learnedQuestions,
            int completionPercent,
            String status,
            int needReviewCount,
            int criticalStudiedQuestions,
            Instant lastStudiedAt
    ) {}
}