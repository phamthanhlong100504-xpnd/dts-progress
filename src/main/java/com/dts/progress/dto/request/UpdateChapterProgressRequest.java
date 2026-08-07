package com.dts.progress.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateChapterProgressRequest(
        @NotNull
        Integer chapterId,

        @NotBlank
        String chapterName,

        @NotNull @Min(1)
        Integer questionsTotal,

        @NotNull @Min(1)
        Integer questionsCount,

        @NotNull @Min(0)
        Integer correctCount,

        @Min(0)
        Integer wrongCount
) {}
