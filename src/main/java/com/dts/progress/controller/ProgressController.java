package com.dts.progress.controller;

import com.dts.progress.dto.request.LogStudySessionRequest;
import com.dts.progress.dto.request.UpdateChapterProgressRequest;
import com.dts.progress.dto.response.*;
import com.dts.progress.security.JwtUserDetails;
import com.dts.progress.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
@Tag(name = "Progress", description = "Theo dõi tiến độ học tập, thống kê và lịch sử thi")
@SecurityRequirement(name = "BearerAuth")
public class ProgressController {

    private final ProgressService progressService;

    // ==================== DASHBOARD ====================

    @GetMapping("/dashboard")
    @Operation(summary = "Get user's learning dashboard with overall statistics")
    public ApiResponse<DashboardResponse> getDashboard(
            @AuthenticationPrincipal JwtUserDetails user) {
        return ApiResponse.ok(progressService.getDashboard(user.userId()));
    }

    // ==================== CHAPTER PROGRESS ====================

    @GetMapping("/chapters")
    @Operation(summary = "Get progress for all chapters")
    public ApiResponse<List<ChapterProgressResponse>> getChapterProgress(
            @AuthenticationPrincipal JwtUserDetails user) {
        return ApiResponse.ok(progressService.getChapterProgress(user.userId()));
    }

    @PatchMapping("/chapters")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update progress for a chapter (record answers)")
    public ApiResponse<ChapterProgressResponse> updateChapterProgress(
            @AuthenticationPrincipal JwtUserDetails user,
            @Valid @RequestBody UpdateChapterProgressRequest request) {
        return ApiResponse.ok(progressService.updateChapterProgress(user.userId(), request));
    }

    // ==================== STREAK ====================

    @GetMapping("/streaks")
    @Operation(summary = "Get study streak information")
    public ApiResponse<StreakResponse> getStreak(
            @AuthenticationPrincipal JwtUserDetails user) {
        return ApiResponse.ok(progressService.getStreak(user.userId()));
    }

    // ==================== STUDY SESSIONS ====================

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Log a study or exam session")
    public ApiResponse<SessionHistoryResponse> logSession(
            @AuthenticationPrincipal JwtUserDetails user,
            @Valid @RequestBody LogStudySessionRequest request) {
        return ApiResponse.ok(progressService.logStudySession(user.userId(), request));
    }

    @GetMapping("/history")
    @Operation(summary = "Get study session history (paginated)")
    public ApiResponse<Page<SessionHistoryResponse>> getHistory(
            @AuthenticationPrincipal JwtUserDetails user,
            Pageable pageable) {
        return ApiResponse.ok(progressService.getHistory(user.userId(), pageable));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent study sessions")
    public ApiResponse<List<SessionHistoryResponse>> getRecent(
            @AuthenticationPrincipal JwtUserDetails user,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(progressService.getRecentSessions(user.userId(), limit));
    }
}
