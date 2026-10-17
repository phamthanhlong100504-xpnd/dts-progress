package com.dts.progress.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PracticeQuestionProgressEvent(
        UUID eventId,
        String eventType,
        Integer schemaVersion,
        String source,
        Long eventSequence,
        UUID userId,
        String licenseClass,
        String bankVersion,
        Integer questionId,
        Integer chapterId,
        Boolean critical,
        String dimension,
        Boolean learned,
        Boolean needsReview,
        Instant studiedAt
) {}
