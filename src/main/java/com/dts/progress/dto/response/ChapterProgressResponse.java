package com.dts.progress.dto.response;

import java.time.Instant;

public record ChapterProgressResponse(
        Integer chapterId,
        String chapterName,
        String status,
        int questionsAnswered,
        int questionsTotal,
        int correctCount,
        Double score,
        Double completionPercent,
        Instant startedAt,
        Instant completedAt,
        Instant lastStudiedAt
) {}
