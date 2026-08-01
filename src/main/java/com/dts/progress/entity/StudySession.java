package com.dts.progress.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "study_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "exam_id")
    private UUID examId;

    // ========== Session Details ==========

    @Column(name = "session_type", length = 20, nullable = false)
    private String sessionType;  // EXAM, PRACTICE

    @Column(name = "exam_type", length = 10)
    private String examType;     // A1, A2, B1, B2, etc.

    @Column(name = "mode", length = 10)
    private String mode;         // EXAM, PRACTICE

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "IN_PROGRESS";  // IN_PROGRESS, COMPLETED, TIMEOUT

    // ========== Results ==========

    @Column(name = "questions_count")
    private Integer questionsCount;

    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(name = "wrong_count")
    private Integer wrongCount;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "passed")
    private Boolean passed;

    // ========== Time Tracking ==========

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    // ========== Audit ==========

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    // ========== Convenience Methods ==========

    public void finish(int correct, int wrong, int totalQuestions, int durationSecs) {
        this.status = "COMPLETED";
        this.correctCount = correct;
        this.wrongCount = wrong;
        this.questionsCount = totalQuestions;
        this.durationSeconds = durationSecs;
        this.score = totalQuestions > 0
                ? BigDecimal.valueOf(Math.round((double) correct / totalQuestions * 10000.0) / 100.0)
                : BigDecimal.ZERO;
        this.passed = correct >= 21;  // Standard DTS passing threshold
        this.completedAt = Instant.now();
    }

    public void timeout() {
        this.status = "TIMEOUT";
        this.completedAt = Instant.now();
    }
}
