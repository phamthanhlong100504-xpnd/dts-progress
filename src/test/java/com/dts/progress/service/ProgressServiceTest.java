package com.dts.progress.service;

import com.dts.progress.dto.request.LogStudySessionRequest;
import com.dts.progress.dto.request.UpdateChapterProgressRequest;
import com.dts.progress.dto.response.*;
import com.dts.progress.entity.ChapterProgress;
import com.dts.progress.entity.StudySession;
import com.dts.progress.entity.UserProgress;
import com.dts.progress.enums.ChapterStatus;
import com.dts.progress.mapper.ProgressMapper;
import com.dts.progress.repository.ChapterProgressRepository;
import com.dts.progress.repository.StudySessionRepository;
import com.dts.progress.repository.UserProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private UserProgressRepository userProgressRepository;
    @Mock
    private ChapterProgressRepository chapterProgressRepository;
    @Mock
    private StudySessionRepository studySessionRepository;
    @Mock
    private ProgressMapper progressMapper;

    @InjectMocks
    private ProgressService progressService;

    private UUID userId;
    private UserProgress userProgress;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userProgress = new UserProgress();
        userProgress.setUserId(userId);
        userProgress.setTotalExams(10);
        userProgress.setTotalPracticeSessions(5);
        userProgress.setTotalStudyTimeSeconds(3600L);
        userProgress.setTotalQuestionsAnswered(100);
        userProgress.setTotalCorrectAnswers(80);
        userProgress.setAverageScore(new BigDecimal("80.0"));
        userProgress.setCurrentStreak(2);
        userProgress.setLongestStreak(5);
        userProgress.setLastStudyDate(LocalDate.now());
    }

    @Test
    void testGetDashboard() {
        when(userProgressRepository.findByUserId(userId)).thenReturn(Optional.of(userProgress));
        when(studySessionRepository.countByUserIdAndSessionType(userId, "EXAM")).thenReturn(10L);
        
        StudySession examSession = new StudySession();
        examSession.setStatus("COMPLETED");
        examSession.setSessionType("EXAM");
        examSession.setPassed(true);

        when(studySessionRepository.findByUserIdAndStartedAtAfterOrderByStartedAtDesc(eq(userId), any(Instant.class)))
                .thenReturn(List.of(examSession));

        ChapterProgress cp = new ChapterProgress();
        cp.setStatus(ChapterStatus.COMPLETED);
        when(chapterProgressRepository.findByUserIdOrderByChapterId(userId)).thenReturn(List.of(cp));

        DashboardResponse response = progressService.getDashboard(userId);

        assertNotNull(response);
        assertEquals(10, response.totalExams());
        assertEquals(1, response.examsPassed());
        assertEquals(0, response.examsFailed());
        assertEquals(100.0, response.passRate());
        assertEquals(1, response.chaptersTotal());
        assertEquals(1, response.chaptersCompleted());
        assertEquals(100.0, response.chaptersProgressPercent());
    }

    @Test
    void testUpdateChapterProgress() {
        UpdateChapterProgressRequest request = new UpdateChapterProgressRequest(1, "Chapter 1", 10, 5, 4, 1);
        
        ChapterProgress cp = new ChapterProgress();
        cp.setChapterId(1);
        cp.setQuestionsTotal(10);
        cp.setQuestionsAnswered(0);
        cp.setCorrectCount(0);
        cp.setStatus(ChapterStatus.NOT_STARTED);

        when(chapterProgressRepository.findByUserIdAndChapterId(userId, 1)).thenReturn(Optional.of(cp));
        when(chapterProgressRepository.save(any(ChapterProgress.class))).thenReturn(cp);
        when(progressMapper.toChapterProgressResponse(any())).thenReturn(
                new ChapterProgressResponse(1, "Chapter 1", "IN_PROGRESS", 5, 10, 4, 40.0, 50.0, Instant.now(), null, Instant.now())
        );

        ChapterProgressResponse response = progressService.updateChapterProgress(userId, request);
        assertNotNull(response);
        assertEquals(5, cp.getQuestionsAnswered());
        assertEquals(4, cp.getCorrectCount());
        verify(chapterProgressRepository).save(cp);
    }

    @Test
    void testLogStudySession() {
        LogStudySessionRequest request = new LogStudySessionRequest("EXAM", "B2", "EXAM", UUID.randomUUID(), 25, 22, 3, 600);
        when(userProgressRepository.findByUserId(userId)).thenReturn(Optional.of(userProgress));
        
        when(studySessionRepository.save(any(StudySession.class))).thenAnswer(i -> {
            StudySession s = i.getArgument(0);
            s.setScore(new BigDecimal("88"));
            return s;
        });

        when(progressMapper.toSessionHistoryResponse(any())).thenReturn(
                new SessionHistoryResponse(UUID.randomUUID(), request.examId(), "EXAM", "B2", "EXAM", "COMPLETED", 25, 22, 3, 88.0, true, 600, Instant.now(), Instant.now())
        );

        SessionHistoryResponse response = progressService.logStudySession(userId, request);

        assertNotNull(response);
        assertEquals(11, userProgress.getTotalExams());
        assertEquals(125, userProgress.getTotalQuestionsAnswered());
        assertEquals(102, userProgress.getTotalCorrectAnswers());
        assertEquals(4200L, userProgress.getTotalStudyTimeSeconds());
        verify(userProgressRepository).save(userProgress);
    }

    @Test
    void testGetStreak() {
        when(userProgressRepository.findByUserId(userId)).thenReturn(Optional.of(userProgress));
        StreakResponse response = progressService.getStreak(userId);
        assertNotNull(response);
        assertTrue(response.studiedToday());
        assertEquals(2, response.currentStreak());
    }
}
