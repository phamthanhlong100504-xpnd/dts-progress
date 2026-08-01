package com.dts.progress.entity;

import com.dts.progress.enums.ChapterStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chapter_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "chapter_id", nullable = false)
    private Integer chapterId;

    @Column(name = "chapter_name", length = 200, nullable = false)
    private String chapterName;

    // ========== Progress Status ==========

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private ChapterStatus status = ChapterStatus.NOT_STARTED;

    @Column(name = "questions_answered", nullable = false)
    @Builder.Default
    private Integer questionsAnswered = 0;

    @Column(name = "questions_total", nullable = false)
    @Builder.Default
    private Integer questionsTotal = 0;

    @Column(name = "correct_count", nullable = false)
    @Builder.Default
    private Integer correctCount = 0;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    // ========== Timestamps ==========

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_studied_at")
    private Instant lastStudiedAt;

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

    public void start() {
        if (status == ChapterStatus.NOT_STARTED) {
            status = ChapterStatus.IN_PROGRESS;
            startedAt = Instant.now();
        }
        lastStudiedAt = Instant.now();
    }

    public void complete() {
        status = ChapterStatus.COMPLETED;
        completedAt = Instant.now();
        lastStudiedAt = Instant.now();
    }
}
