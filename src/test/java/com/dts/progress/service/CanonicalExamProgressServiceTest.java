package com.dts.progress.service;

import com.dts.progress.dto.event.PracticeExamFinishedEvent;
import com.dts.progress.dto.response.SessionHistoryResponse;
import com.dts.progress.entity.StudySession;
import com.dts.progress.mapper.ProgressMapper;
import com.dts.progress.repository.StudySessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CanonicalExamProgressServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private StudySessionRepository studySessionRepository;

    @Mock
    private ProgressMapper mapper;

    @InjectMocks
    private CanonicalExamProgressService service;

    @Test
    void duplicateDeliveryDoesNotWriteSessionAgain() {
        UUID userId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();

        Instant started =
                Instant.parse("2026-10-09T10:00:00Z");
        Instant completed =
                started.plusSeconds(600);

        PracticeExamFinishedEvent event =
                new PracticeExamFinishedEvent(
                        examId,
                        "EXAM_FINISHED",
                        1,
                        "dts-practice",
                        userId,
                        examId,
                        "A1",
                        "dts-2026-600-v1",
                        "EXAM",
                        "COMPLETED",
                        25,
                        22,
                        3,
                        3,
                        0,
                        88,
                        true,
                        600,
                        started,
                        completed
                );

        StudySession existing = StudySession.builder()
                .userId(userId)
                .examId(examId)
                .sessionType("EXAM")
                .examType("A1")
                .mode("EXAM")
                .status("COMPLETED")
                .questionsCount(25)
                .correctCount(22)
                .wrongCount(3)
                .score(BigDecimal.valueOf(88))
                .passed(true)
                .durationSeconds(600)
                .startedAt(started)
                .completedAt(completed)
                .build();

        SessionHistoryResponse response =
                mock(SessionHistoryResponse.class);

        when(studySessionRepository
                .findByUserIdAndExamId(userId, examId))
                .thenReturn(Optional.of(existing));

        when(mapper.toSessionHistoryResponse(existing))
                .thenReturn(response);

        assertSame(response, service.apply(event));
        assertSame(response, service.apply(event));

        verify(studySessionRepository, never())
                .saveAndFlush(any());

        verify(jdbcTemplate, times(2))
                .queryForObject(
                        anyString(),
                        eq(UUID.class),
                        eq(userId)
                );
    }
}