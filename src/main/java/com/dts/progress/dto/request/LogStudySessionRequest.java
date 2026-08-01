package com.dts.progress.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LogStudySessionRequest(
        @NotNull
        String sessionType,  // EXAM, PRACTICE

        String examType,     // A1, A2, B1, B2, etc.

        String mode,         // EXAM, PRACTICE

        UUID examId,         // nullable for practice sessions

        @NotNull @Min(1)
        Integer questionsCount,

        @NotNull @Min(0)
        Integer correctCount,

        @NotNull @Min(0)
        Integer wrongCount,

        @NotNull @Min(0)
        Integer durationSeconds
) {}
