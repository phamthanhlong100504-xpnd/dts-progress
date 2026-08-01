package com.dts.progress.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "username", length = 100, nullable = false)
    private String username;

    // ========== Overall Stats ==========

    @Column(name = "total_exams", nullable = false)
    @Builder.Default
    private Integer totalExams = 0;

    @Column(name = "total_practice_sessions", nullable = false)
    @Builder.Default
    private Integer totalPracticeSessions = 0;

    @Column(name = "total_study_time_seconds", nullable = false)
    @Builder.Default
    private Long totalStudyTimeSeconds = 0L;

    @Column(name = "total_questions_answered", nullable = false)
    @Builder.Default
    private Integer totalQuestionsAnswered = 0;

    @Column(name = "total_correct_answers", nullable = false)
    @Builder.Default
    private Integer totalCorrectAnswers = 0;

    @Column(name = "average_score", precision = 5, scale = 2)
    private BigDecimal averageScore;

    // ========== Streak Tracking ==========

    @Column(name = "current_streak", nullable = false)
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "longest_streak", nullable = false)
    @Builder.Default
    private Integer longestStreak = 0;

    @Column(name = "last_study_date")
    private LocalDate lastStudyDate;

    // ========== Audit ==========

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 1L;

    // ========== Convenience Methods ==========

    public void recordStudy(LocalDate studyDate) {
        if (lastStudyDate == null) {
            currentStreak = 1;
        } else if (lastStudyDate.equals(studyDate)) {
            // Same day, no streak change
        } else if (lastStudyDate.plusDays(1).equals(studyDate)) {
            currentStreak++;
        } else {
            currentStreak = 1;
        }
        lastStudyDate = studyDate;
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
        }
    }

    public void updateAverageScore(int newScore) {
        int total = totalExams + totalPracticeSessions;
        if (total == 0) {
            averageScore = BigDecimal.valueOf(newScore);
        } else {
            BigDecimal prev = averageScore != null ? averageScore : BigDecimal.ZERO;
            BigDecimal numerator = prev.multiply(BigDecimal.valueOf(total - 1))
                    .add(BigDecimal.valueOf(newScore));
            averageScore = numerator.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        }
    }
}
