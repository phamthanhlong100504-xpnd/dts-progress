package com.dts.progress.dto.response;

import java.time.LocalDate;

public record DashboardResponse(
        // Overall stats
        int totalExams,
        int totalPracticeSessions,
        long totalStudyTimeSeconds,
        int totalQuestionsAnswered,
        int totalCorrectAnswers,
        Double averageScore,

        // Pass rate
        int examsPassed,
        int examsFailed,
        Double passRate,

        // Streak
        int currentStreak,
        int longestStreak,
        LocalDate lastStudyDate,

        // Chapter progress
        int chaptersTotal,
        int chaptersCompleted,
        int chaptersInProgress,
        double chaptersProgressPercent
) {}
