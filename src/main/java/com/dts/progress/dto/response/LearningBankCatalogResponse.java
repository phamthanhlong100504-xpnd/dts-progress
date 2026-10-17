package com.dts.progress.dto.response;

import java.util.List;

public record LearningBankCatalogResponse(
        String licenseClass,
        String bankVersion,
        int totalQuestions,
        int criticalQuestions,
        List<Chapter> chapters
) {
    public record Chapter(
            int chapterId,
            String chapterName,
            int totalQuestions,
            int criticalQuestions
    ) {}
}