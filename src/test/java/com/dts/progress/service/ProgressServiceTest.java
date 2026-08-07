package com.dts.progress.service;

import com.dts.progress.dto.request.LogStudySessionRequest;
import com.dts.progress.dto.request.UpdateChapterProgressRequest;
import com.dts.progress.dto.response.ChapterProgressResponse;
import com.dts.progress.dto.response.DashboardResponse;
import com.dts.progress.dto.response.SessionHistoryResponse;
import com.dts.progress.dto.response.StreakResponse;
import com.dts.progress.entity.UserProgress;
import com.dts.progress.exception.BusinessException;
import com.dts.progress.repository.UserProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka
@Transactional
class ProgressServiceTest {

    @Autowired
    private ProgressService progressService;

    @Autowired
    private UserProgressRepository userProgressRepository;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should return dashboard for new user with zero stats")
    void dashboardForNewUser() {
        DashboardResponse dashboard = progressService.getDashboard(testUserId);

        assertThat(dashboard).isNotNull();
        assertThat(dashboard.totalExams()).isEqualTo(0);
        assertThat(dashboard.totalPracticeSessions()).isEqualTo(0);
        assertThat(dashboard.totalStudyTimeSeconds()).isEqualTo(0);
        assertThat(dashboard.currentStreak()).isEqualTo(0);
        assertThat(dashboard.longestStreak()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should log a study session and update user progress stats")
    void logStudySession() {
        LogStudySessionRequest request = new LogStudySessionRequest(
                "EXAM", "A1", "EXAM", null, 25, 21, 4, 1200);

        SessionHistoryResponse response = progressService.logStudySession(testUserId, request);

        assertThat(response).isNotNull();
        assertThat(response.sessionType()).isEqualTo("EXAM");
        assertThat(response.questionsCount()).isEqualTo(25);
        assertThat(response.correctCount()).isEqualTo(21);
        assertThat(response.wrongCount()).isEqualTo(4);
        assertThat(response.passed()).isTrue();
        assertThat(response.score()).isEqualTo(84.0);
        assertThat(response.durationSeconds()).isEqualTo(1200);

        // Verify user progress was updated
        UserProgress up = userProgressRepository.findByUserId(testUserId).orElseThrow();
        assertThat(up.getTotalExams()).isEqualTo(1);
        assertThat(up.getTotalQuestionsAnswered()).isEqualTo(25);
        assertThat(up.getTotalCorrectAnswers()).isEqualTo(21);
        assertThat(up.getTotalStudyTimeSeconds()).isEqualTo(1200);
        assertThat(up.getCurrentStreak()).isEqualTo(1);
        assertThat(up.getLastStudyDate()).isEqualTo(java.time.LocalDate.now());
    }

    @Test
    @DisplayName("Should log a practice session and not count as exam")
    void logPracticeSession() {
        LogStudySessionRequest request = new LogStudySessionRequest(
                "PRACTICE", "A1", "PRACTICE", null, 10, 8, 2, 600);

        SessionHistoryResponse response = progressService.logStudySession(testUserId, request);

        assertThat(response.sessionType()).isEqualTo("PRACTICE");

        UserProgress up = userProgressRepository.findByUserId(testUserId).orElseThrow();
        assertThat(up.getTotalExams()).isEqualTo(0);
        assertThat(up.getTotalPracticeSessions()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should update chapter progress")
    void updateChapterProgress() {
        UpdateChapterProgressRequest request = new UpdateChapterProgressRequest(
                1, "Khái niệm và quy tắc", 25, 10, 8, 2);

        ChapterProgressResponse response = progressService.updateChapterProgress(testUserId, request);

        assertThat(response).isNotNull();
        assertThat(response.chapterId()).isEqualTo(1);
        assertThat(response.chapterName()).isEqualTo("Khái niệm và quy tắc");
        assertThat(response.status()).isEqualTo("IN_PROGRESS");
        assertThat(response.questionsTotal()).isEqualTo(25);
        assertThat(response.questionsAnswered()).isEqualTo(10);
        assertThat(response.correctCount()).isEqualTo(8);
        assertThat(response.completionPercent()).isEqualTo(40.0);
    }

    @Test
    @DisplayName("Should return streak info")
    void getStreak() {
        StreakResponse streak = progressService.getStreak(testUserId);

        assertThat(streak).isNotNull();
        assertThat(streak.currentStreak()).isEqualTo(0);
        assertThat(streak.longestStreak()).isEqualTo(0);
        assertThat(streak.studiedToday()).isFalse();
    }

    @Test
    @DisplayName("Should return empty chapter progress list for new user")
    void emptyChapterProgress() {
        List<ChapterProgressResponse> chapters = progressService.getChapterProgress(testUserId);

        assertThat(chapters).isNotNull();
        assertThat(chapters).isEmpty();
    }

    @Test
    @DisplayName("Should build consecutive streaks")
    void buildStreak() {
        // Log two sessions on consecutive calls
        LogStudySessionRequest request = new LogStudySessionRequest(
                "EXAM", "A1", "EXAM", null, 25, 21, 4, 1200);

        progressService.logStudySession(testUserId, request);

        UserProgress up = userProgressRepository.findByUserId(testUserId).orElseThrow();
        assertThat(up.getCurrentStreak()).isEqualTo(1);
        assertThat(up.getLongestStreak()).isEqualTo(1);

        // Second session same day — streak shouldn't double count
        progressService.logStudySession(testUserId, request);

        up = userProgressRepository.findByUserId(testUserId).orElseThrow();
        // Same day, so streak stays at 1
        assertThat(up.getCurrentStreak()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should auto-complete chapter when all questions answered")
    void autoCompleteChapter() {
        UpdateChapterProgressRequest request = new UpdateChapterProgressRequest(
                1, "Biển báo đường bộ", 25, 25, 21, 4);

        ChapterProgressResponse response = progressService.updateChapterProgress(testUserId, request);

        assertThat(response.questionsAnswered()).isEqualTo(25);
        assertThat(response.correctCount()).isEqualTo(21);
        assertThat(response.completionPercent()).isEqualTo(100.0);
        assertThat(response.score()).isEqualTo(84.0);
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.completedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should clamp answered/correct to questionsTotal")
    void clampBeyondTotal() {
        // Client reports more answered than the chapter contains
        UpdateChapterProgressRequest request = new UpdateChapterProgressRequest(
                1, "Khởi hành", 25, 30, 20, 10);

        ChapterProgressResponse response = progressService.updateChapterProgress(testUserId, request);

        assertThat(response.questionsAnswered()).isEqualTo(25);
        assertThat(response.correctCount()).isEqualTo(20);
        assertThat(response.completionPercent()).isEqualTo(100.0);
        assertThat(response.score()).isEqualTo(80.0);
        assertThat(response.status()).isEqualTo("COMPLETED");

        // Re-sending accumulates but is bounded by the cap — never exceeds total
        progressService.updateChapterProgress(testUserId, request);
        ChapterProgressResponse afterResend = progressService.getChapterProgress(testUserId).get(0);
        assertThat(afterResend.questionsAnswered()).isEqualTo(25);
        assertThat(afterResend.correctCount()).isEqualTo(25); // 20+20 clamped to answered
        assertThat(afterResend.completionPercent()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should reject correctCount greater than questionsCount")
    void rejectImpossibleCounts() {
        UpdateChapterProgressRequest request = new UpdateChapterProgressRequest(
                1, "Khởi hành", 25, 10, 15, 0);

        assertThatThrownBy(() -> progressService.updateChapterProgress(testUserId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("correctCount");
    }
}
