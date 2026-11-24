package com.dts.progress.dto.response;

import java.util.List;

public final class LearnerProgressModels {
    private LearnerProgressModels() {}

    public record LearnedAnswer(
            int questionId,
            String selectedAnswer,
            boolean isCorrect,
            String learnedAt,
            String lastAnsweredAt
    ) {}

    public record LearningCursor(
            int chapterId,
            Integer lastQuestionId,
            Integer resumeQuestionId,
            String lastStudiedAt
    ) {}

    public record Bookmarks(
            int chapterId,
            String licenseClass,
            List<Integer> questionIds
    ) {}

    public record BookmarkResponse(
            int chapterId,
            int questionId,
            String licenseClass,
            boolean bookmarked
    ) {}

    public record CriticalItem(
            int questionId,
            int chapterId,
            boolean learned,
            String selectedAnswer,
            Boolean isCorrect,
            String lastStudiedAt
    ) {}

    public record CriticalOverview(
            String licenseClass,
            int totalQuestions,
            int learnedQuestions,
            int total,
            int learned,
            int unlearned,
            double completionPercent,
            int currentPosition,
            boolean hasProgress,
            boolean completed,
            String lastStudiedAt,
            List<CriticalItem> questions
    ) {}

    public record WrongItem(
            int questionId,
            int chapterId,
            boolean isCritical,
            String lastWrongAnswer,
            String lastWrongAt,
            String lastAnsweredAt
    ) {}

    public record WrongOverview(
            String licenseClass,
            int totalWrongQuestions,
            boolean hasWrongQuestions,
            int total,
            int totalWrong,
            List<WrongItem> questions
    ) {}

    public record ChapterOverviewItem(
            int chapterId,
            int totalQuestions,
            int learnedQuestions,
            double completionPercent,
            String status,
            Integer lastQuestionId,
            Integer resumeQuestionId,
            String lastStudiedAt
    ) {}

    public record ChapterOverview(
            String licenseClass,
            String bankVersion,
            int totalQuestions,
            int learnedQuestions,
            double completionPercent,
            List<ChapterOverviewItem> chapters
    ) {}

    public record AnswerRecordResponse(
            int questionId,
            String selectedAnswer,
            boolean isCorrect,
            String correctAnswer,
            String explanation,
            String answeredAt
    ) {}
}
