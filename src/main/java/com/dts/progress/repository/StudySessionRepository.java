package com.dts.progress.repository;

import com.dts.progress.entity.StudySession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, UUID> {

    Page<StudySession> findByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);

    List<StudySession> findByUserIdAndStartedAtAfterOrderByStartedAtDesc(UUID userId, Instant since);

    long countByUserIdAndSessionType(UUID userId, String sessionType);

    List<StudySession> findByStatusAndStartedAtBefore(String status, Instant before);
}
