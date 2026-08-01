package com.dts.progress.repository;

import com.dts.progress.entity.ChapterProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChapterProgressRepository extends JpaRepository<ChapterProgress, UUID> {

    List<ChapterProgress> findByUserIdOrderByChapterId(UUID userId);

    Optional<ChapterProgress> findByUserIdAndChapterId(UUID userId, Integer chapterId);

    long countByUserIdAndStatus(UUID userId, String status);

    boolean existsByUserIdAndChapterId(UUID userId, Integer chapterId);
}
