package com.dts.progress.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PracticeExamFinishedEvent(
        UUID eventId,
        String eventType,
        Integer schemaVersion,
        String source,
        UUID userId,
        UUID examId,
        String licenseClass,
        String bankVersion,
        String mode,
        String status,
        Integer totalQuestions,
        Integer correctCount,
        Integer wrongCountLegacy,
        Integer answeredWrongCount,
        Integer unansweredCount,
        Integer score,
        Boolean passed,
        Integer durationSeconds,
        Instant startedAt,
        Instant completedAt
) {}