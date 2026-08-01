package com.dts.progress.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SessionHistoryResponse(
        UUID id,
        UUID examId,
        String sessionType,
        String examType,
        String mode,
        String status,
        Integer questionsCount,
        Integer correctCount,
        Integer wrongCount,
        Double score,
        Boolean passed,
        Integer durationSeconds,
        Instant startedAt,
        Instant completedAt
) {}
