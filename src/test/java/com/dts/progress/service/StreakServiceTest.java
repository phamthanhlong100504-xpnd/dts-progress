package com.dts.progress.service;

import com.dts.progress.entity.UserProgress;
import com.dts.progress.repository.UserProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    @Mock
    private UserProgressRepository userProgressRepository;

    @InjectMocks
    private ProgressService progressService;

    private UUID userId;
    private UserProgress userProgress;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userProgress = new UserProgress();
        userProgress.setUserId(userId);
        userProgress.setCurrentStreak(0);
        userProgress.setLongestStreak(0);
        userProgress.setLastStudyDate(null);
    }

    @Test
    @DisplayName("Streak - First study day initializes currentStreak to 1")
    void testFirstStudyDay_InitializesStreak() {
        when(userProgressRepository.findByUserId(userId)).thenReturn(Optional.of(userProgress));

        progressService.logStudySession(userId, new com.dts.progress.dto.request.LogStudySessionRequest(
                "PRACTICE", "B2", "PRACTICE", UUID.randomUUID(), 10, 8, 2, 300));

        assertEquals(1, userProgress.getCurrentStreak());
        assertEquals(1, userProgress.getLongestStreak());
        assertEquals(LocalDate.now(), userProgress.getLastStudyDate());
    }

    @Test
    @DisplayName("Streak - Consecutive days increments currentStreak")
    void testConsecutiveDays_IncrementsStreak() {
        userProgress.setCurrentStreak(2);
        userProgress.setLongestStreak(3);
        userProgress.setLastStudyDate(LocalDate.now().minusDays(1));

        when(userProgressRepository.findByUserId(userId)).thenReturn(Optional.of(userProgress));

        progressService.logStudySession(userId, new com.dts.progress.dto.request.LogStudySessionRequest(
                "PRACTICE", "B2", "PRACTICE", UUID.randomUUID(), 10, 8, 2, 300));

        assertEquals(3, userProgress.getCurrentStreak());
        assertEquals(3, userProgress.getLongestStreak());
        assertEquals(LocalDate.now(), userProgress.getLastStudyDate());
    }

    @Test
    @DisplayName("Streak - Same day study does not change streak")
    void testSameDayStudy_NoStreakChange() {
        userProgress.setCurrentStreak(5);
        userProgress.setLongestStreak(7);
        userProgress.setLastStudyDate(LocalDate.now());

        when(userProgressRepository.findByUserId(userId)).thenReturn(Optional.of(userProgress));

        progressService.logStudySession(userId, new com.dts.progress.dto.request.LogStudySessionRequest(
                "PRACTICE", "B2", "PRACTICE", UUID.randomUUID(), 10, 8, 2, 300));

        assertEquals(5, userProgress.getCurrentStreak());
        assertEquals(7, userProgress.getLongestStreak());
        assertEquals(LocalDate.now(), userProgress.getLastStudyDate());
    }

    @Test
    @DisplayName("Streak - Gap of more than 1 day resets currentStreak to 1")
    void testGapResetsStreak() {
        userProgress.setCurrentStreak(10);
        userProgress.setLongestStreak(10);
        userProgress.setLastStudyDate(LocalDate.now().minusDays(3));

        when(userProgressRepository.findByUserId(userId)).thenReturn(Optional.of(userProgress));

        progressService.logStudySession(userId, new com.dts.progress.dto.request.LogStudySessionRequest(
                "PRACTICE", "B2", "PRACTICE", UUID.randomUUID(), 10, 8, 2, 300));

        assertEquals(1, userProgress.getCurrentStreak());
        assertEquals(10, userProgress.getLongestStreak());
        assertEquals(LocalDate.now(), userProgress.getLastStudyDate());
    }

    @Test
    @DisplayName("Streak - currentStreak exceeds longestStreak updates longestStreak")
    void testCurrentExceedsLongest_UpdatesLongest() {
        userProgress.setCurrentStreak(5);
        userProgress.setLongestStreak(5);
        userProgress.setLastStudyDate(LocalDate.now().minusDays(1));

        when(userProgressRepository.findByUserId(userId)).thenReturn(Optional.of(userProgress));

        progressService.logStudySession(userId, new com.dts.progress.dto.request.LogStudySessionRequest(
                "PRACTICE", "B2", "PRACTICE", UUID.randomUUID(), 10, 8, 2, 300));

        assertEquals(6, userProgress.getCurrentStreak());
        assertEquals(6, userProgress.getLongestStreak());
    }

    @Test
    @DisplayName("getStreak - Returns correct streak response")
    void testGetStreak_ReturnsResponse() {
        userProgress.setCurrentStreak(4);
        userProgress.setLongestStreak(8);
        userProgress.setLastStudyDate(LocalDate.now());

        when(userProgressRepository.findByUserId(userId)).thenReturn(Optional.of(userProgress));

        var response = progressService.getStreak(userId);

        assertEquals(4, response.currentStreak());
        assertEquals(8, response.longestStreak());
        assertEquals(LocalDate.now(), response.lastStudyDate());
        assertTrue(response.studiedToday());
    }

    @Test
    @DisplayName("getStreak - studiedToday false when lastStudyDate is yesterday")
    void testGetStreak_NotStudiedToday() {
        userProgress.setCurrentStreak(4);
        userProgress.setLongestStreak(8);
        userProgress.setLastStudyDate(LocalDate.now().minusDays(1));

        when(userProgressRepository.findByUserId(userId)).thenReturn(Optional.of(userProgress));

        var response = progressService.getStreak(userId);

        assertFalse(response.studiedToday());
    }

    @Test
    @DisplayName("getStreak - Creates new UserProgress if not exists")
    void testGetStreak_CreatesNewUserProgress() {
        when(userProgressRepository.findByUserId(userId)).thenReturn(Optional.empty());

        var newProgress = new UserProgress();
        newProgress.setUserId(userId);
        newProgress.setCurrentStreak(0);
        newProgress.setLongestStreak(0);
        when(userProgressRepository.save(any(UserProgress.class))).thenReturn(newProgress);

        var response = progressService.getStreak(userId);

        assertNotNull(response);
        assertEquals(0, response.currentStreak());
        assertFalse(response.studiedToday());
    }
}