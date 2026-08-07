package com.dts.progress.service;

import com.dts.progress.dto.request.LogStudySessionRequest;
import com.dts.progress.dto.request.UpdateChapterProgressRequest;
import com.dts.progress.dto.response.*;
import com.dts.progress.entity.ChapterProgress;
import com.dts.progress.entity.StudySession;
import com.dts.progress.entity.UserProgress;
import com.dts.progress.enums.ChapterStatus;
import com.dts.progress.exception.BusinessException;
import com.dts.progress.mapper.ProgressMapper;
import com.dts.progress.repository.ChapterProgressRepository;
import com.dts.progress.repository.StudySessionRepository;
import com.dts.progress.repository.UserProgressRepository;
import com.dts.progress.security.JwtUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProgressService {

    private final UserProgressRepository userProgressRepository;
    private final ChapterProgressRepository chapterProgressRepository;
    private final StudySessionRepository studySessionRepository;
    private final ProgressMapper progressMapper;

    private static final int PASSING_SCORE = 21; // Điểm đỗ: 21/25 = 84%

    // ==================== DASHBOARD ====================

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID userId) {
        UserProgress up = getOrCreateUserProgress(userId);
        long examsPassed = studySessionRepository.countByUserIdAndSessionType(userId, "EXAM");
        long examsFailed = 0; // Will be refined below

        List<StudySession> examSessions = studySessionRepository
                .findByUserIdAndStartedAtAfterOrderByStartedAtDesc(userId, Instant.EPOCH);
        long completedExams = examSessions.stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()) && "EXAM".equals(s.getSessionType()))
                .count();
        long passedExams = examSessions.stream()
                .filter(s -> "COMPLETED".equals(s.getStatus())
                        && "EXAM".equals(s.getSessionType())
                        && Boolean.TRUE.equals(s.getPassed()))
                .count();
        long failedExams = completedExams - passedExams;

        Double passRate = completedExams > 0
                ? Math.round((double) passedExams / completedExams * 10000.0) / 100.0
                : null;

        List<ChapterProgress> chapters = chapterProgressRepository.findByUserIdOrderByChapterId(userId);
        int chaptersCompleted = (int) chapters.stream()
                .filter(c -> c.getStatus() == ChapterStatus.COMPLETED).count();
        int chaptersInProgress = (int) chapters.stream()
                .filter(c -> c.getStatus() == ChapterStatus.IN_PROGRESS).count();
        double chaptersProgressPercent = !chapters.isEmpty()
                ? Math.round((double) chaptersCompleted / chapters.size() * 10000.0) / 100.0
                : 0.0;

        return new DashboardResponse(
                up.getTotalExams(),
                up.getTotalPracticeSessions(),
                up.getTotalStudyTimeSeconds(),
                up.getTotalQuestionsAnswered(),
                up.getTotalCorrectAnswers(),
                up.getAverageScore() != null ? up.getAverageScore().doubleValue() : null,
                (int) passedExams,
                (int) failedExams,
                passRate,
                up.getCurrentStreak(),
                up.getLongestStreak(),
                up.getLastStudyDate(),
                chapters.size(),
                chaptersCompleted,
                chaptersInProgress,
                chaptersProgressPercent
        );
    }

    // ==================== CHAPTER PROGRESS ====================

    @Transactional(readOnly = true)
    public List<ChapterProgressResponse> getChapterProgress(UUID userId) {
        return chapterProgressRepository.findByUserIdOrderByChapterId(userId).stream()
                .map(progressMapper::toChapterProgressResponse)
                .toList();
    }

    @Transactional
    public ChapterProgressResponse updateChapterProgress(UUID userId, UpdateChapterProgressRequest request) {
        // Sanity: correct answers can't exceed questions answered, and wrong+correct can't exceed answered
        if (request.correctCount() > request.questionsCount()) {
            throw BusinessException.badRequest("correctCount (" + request.correctCount()
                    + ") cannot exceed questionsCount (" + request.questionsCount() + ")");
        }
        if (request.wrongCount() != null
                && request.correctCount() + request.wrongCount() > request.questionsCount()) {
            throw BusinessException.badRequest(
                    "correctCount + wrongCount cannot exceed questionsCount");
        }

        ChapterProgress cp = chapterProgressRepository
                .findByUserIdAndChapterId(userId, request.chapterId())
                .orElseGet(() -> ChapterProgress.builder()
                        .userId(userId)
                        .chapterId(request.chapterId())
                        .chapterName(request.chapterName())
                        .questionsTotal(request.questionsTotal())
                        .status(ChapterStatus.NOT_STARTED)
                        .build());

        cp.setChapterName(request.chapterName());
        cp.setQuestionsTotal(request.questionsTotal());

        if (cp.getStatus() == ChapterStatus.NOT_STARTED) {
            cp.start();
        }

        int total = cp.getQuestionsTotal();
        int answered = Math.min(cp.getQuestionsAnswered() + request.questionsCount(), total);
        int correct = Math.min(cp.getCorrectCount() + request.correctCount(), answered);

        cp.setQuestionsAnswered(answered);
        cp.setCorrectCount(correct);

        if (total > 0) {
            double score = Math.min((double) correct / total * 100.0, 100.0);
            cp.setScore(BigDecimal.valueOf(Math.round(score * 100.0) / 100.0));
        }

        // Auto-complete once every question has been answered at least once
        if (answered >= total) {
            cp.complete();
        }

        cp = chapterProgressRepository.save(cp);
        log.info("Updated chapter progress: userId={}, chapterId={}, status={}", userId, request.chapterId(), cp.getStatus());
        return progressMapper.toChapterProgressResponse(cp);
    }

    // ==================== STREAK ====================

    @Transactional(readOnly = true)
    public StreakResponse getStreak(UUID userId) {
        UserProgress up = getOrCreateUserProgress(userId);
        boolean studiedToday = up.getLastStudyDate() != null
                && up.getLastStudyDate().equals(LocalDate.now());
        return new StreakResponse(
                up.getCurrentStreak(),
                up.getLongestStreak(),
                up.getLastStudyDate(),
                studiedToday
        );
    }

    // ==================== STUDY SESSION ====================

    @Transactional
    public SessionHistoryResponse logStudySession(UUID userId, LogStudySessionRequest request) {
        UserProgress up = getOrCreateUserProgress(userId);

        // Create session
        StudySession session = StudySession.builder()
                .userId(userId)
                .examId(request.examId())
                .sessionType(request.sessionType())
                .examType(request.examType())
                .mode(request.mode())
                .status("COMPLETED")
                .questionsCount(request.questionsCount())
                .correctCount(request.correctCount())
                .wrongCount(request.wrongCount())
                .durationSeconds(request.durationSeconds())
                .startedAt(Instant.now().minusSeconds(request.durationSeconds()))
                .completedAt(Instant.now())
                .build();
        session.finish(request.correctCount(), request.wrongCount(),
                request.questionsCount(), request.durationSeconds());
        session = studySessionRepository.save(session);

        // Update user progress stats
        up.setTotalQuestionsAnswered(up.getTotalQuestionsAnswered() + request.questionsCount());
        up.setTotalCorrectAnswers(up.getTotalCorrectAnswers() + request.correctCount());
        up.setTotalStudyTimeSeconds(up.getTotalStudyTimeSeconds() + request.durationSeconds());

        if ("EXAM".equals(request.sessionType())) {
            up.setTotalExams(up.getTotalExams() + 1);
        } else {
            up.setTotalPracticeSessions(up.getTotalPracticeSessions() + 1);
        }

        // Update score average
        if (session.getScore() != null) {
            up.updateAverageScore(session.getScore().intValue());
        }

        // Update streak
        up.recordStudy(LocalDate.now());

        userProgressRepository.save(up);
        log.info("Logged study session: userId={}, type={}, score={}", userId, request.sessionType(), session.getScore());

        return progressMapper.toSessionHistoryResponse(session);
    }

    // ==================== HISTORY ====================

    @Transactional(readOnly = true)
    public Page<SessionHistoryResponse> getHistory(UUID userId, Pageable pageable) {
        return studySessionRepository.findByUserIdOrderByStartedAtDesc(userId, pageable)
                .map(progressMapper::toSessionHistoryResponse);
    }

    @Transactional(readOnly = true)
    public List<SessionHistoryResponse> getRecentSessions(UUID userId, int limit) {
        return studySessionRepository
                .findByUserIdAndStartedAtAfterOrderByStartedAtDesc(userId, Instant.EPOCH)
                .stream()
                .limit(limit)
                .map(progressMapper::toSessionHistoryResponse)
                .toList();
    }

    // ==================== HELPERS ====================

    private UserProgress getOrCreateUserProgress(UUID userId) {
        return userProgressRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserProgress up = UserProgress.builder()
                            .userId(userId)
                            .username("unknown")
                            .build();
                    return userProgressRepository.save(up);
                });
    }
}
