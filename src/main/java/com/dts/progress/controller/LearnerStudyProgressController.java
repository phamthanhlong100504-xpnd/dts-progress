package com.dts.progress.controller;

import com.dts.progress.dto.response.ApiResponse;
import com.dts.progress.dto.response.LearnerProgressModels.*;
import com.dts.progress.security.JwtUserDetails;
import com.dts.progress.service.LearnerStudyProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Learner Study Progress", description = "Quản lý tiến độ học tập chi tiết của học viên")
@SecurityRequirement(name = "BearerAuth")
public class LearnerStudyProgressController {

    private final LearnerStudyProgressService progressService;

    // 0. Chapter learning overview
    @GetMapping({"/api/v1/progress/chapters/overview", "/api/v1/learning/chapters"})
    @PreAuthorize("hasAuthority('PERM_progress:read')")
    @Operation(summary = "Lấy tổng quan tiến độ học 6 chương lý thuyết")
    public ApiResponse<ChapterOverview> getChapterOverview(
            @AuthenticationPrincipal JwtUserDetails user,
            @RequestParam(defaultValue = "B2") String licenseClass
    ) {
        return ApiResponse.ok(progressService.getChapterOverview(user.userId(), licenseClass));
    }

    // 1. Chapter learned answers
    @GetMapping({"/api/v1/progress/chapters/{chapterId}/answers", "/api/v1/learning/chapters/{chapterId}/answers"})
    @PreAuthorize("hasAuthority('PERM_progress:read')")
    @Operation(summary = "Lấy danh sách các câu đã làm trong chương")
    public ApiResponse<List<LearnedAnswer>> getChapterAnswers(
            @AuthenticationPrincipal JwtUserDetails user,
            @PathVariable int chapterId,
            @RequestParam(defaultValue = "B2") String licenseClass
    ) {
        return ApiResponse.ok(progressService.getChapterAnswers(user.userId(), licenseClass, chapterId));
    }

    @PostMapping({"/api/v1/progress/chapters/{chapterId}/answers", "/api/v1/learning/chapters/{chapterId}/answers"})
    @PreAuthorize("hasAuthority('PERM_progress:update')")
    @Operation(summary = "Ghi nhận kết quả trả lời câu hỏi khi học chương")
    public ApiResponse<AnswerRecordResponse> recordChapterAnswer(
            @AuthenticationPrincipal JwtUserDetails user,
            @PathVariable int chapterId,
            @RequestBody Map<String, Object> body
    ) {
        String license = body.containsKey("licenseClass") ? String.valueOf(body.get("licenseClass")) : "B2";
        int questionId = ((Number) body.get("questionId")).intValue();
        String selectedAnswer = String.valueOf(body.getOrDefault("selectedAnswer", ""));
        boolean isCorrect = Boolean.TRUE.equals(body.get("isCorrect"));
        String correctAnswer = String.valueOf(body.getOrDefault("correctAnswer", ""));
        String explanation = body.containsKey("explanation") && body.get("explanation") != null
                ? String.valueOf(body.get("explanation")) : null;
        boolean isCritical = Boolean.TRUE.equals(body.get("isCritical"));

        return ApiResponse.ok(progressService.recordChapterAnswer(
                user.userId(), license, chapterId, questionId, selectedAnswer, isCorrect, correctAnswer, explanation, isCritical
        ));
    }

    // 2. Chapter cursor
    @GetMapping({"/api/v1/progress/chapters/{chapterId}/cursor", "/api/v1/learning/chapters/{chapterId}/cursor"})
    @PreAuthorize("hasAuthority('PERM_progress:read')")
    @Operation(summary = "Lấy vị trí câu hỏi đang học dở trong chương")
    public ApiResponse<LearningCursor> getChapterCursor(
            @AuthenticationPrincipal JwtUserDetails user,
            @PathVariable int chapterId,
            @RequestParam(defaultValue = "B2") String licenseClass
    ) {
        return ApiResponse.ok(progressService.getChapterCursor(user.userId(), licenseClass, chapterId));
    }

    @PutMapping({"/api/v1/progress/chapters/{chapterId}/cursor", "/api/v1/learning/chapters/{chapterId}/cursor"})
    @PreAuthorize("hasAuthority('PERM_progress:update')")
    @Operation(summary = "Lưu vị trí câu hỏi đang học dở trong chương")
    public ApiResponse<LearningCursor> saveChapterCursor(
            @AuthenticationPrincipal JwtUserDetails user,
            @PathVariable int chapterId,
            @RequestBody Map<String, Object> body
    ) {
        String license = body.containsKey("licenseClass") ? String.valueOf(body.get("licenseClass")) : "B2";
        int questionId = ((Number) body.get("questionId")).intValue();
        return ApiResponse.ok(progressService.saveChapterCursor(user.userId(), license, chapterId, questionId));
    }

    // 3. Chapter bookmarks
    @GetMapping({"/api/v1/progress/chapters/{chapterId}/bookmarks", "/api/v1/learning/chapters/{chapterId}/bookmarks"})
    @PreAuthorize("hasAuthority('PERM_progress:read')")
    @Operation(summary = "Lấy danh sách câu hỏi đã bookmark trong chương")
    public ApiResponse<Bookmarks> getChapterBookmarks(
            @AuthenticationPrincipal JwtUserDetails user,
            @PathVariable int chapterId,
            @RequestParam(defaultValue = "B2") String licenseClass
    ) {
        return ApiResponse.ok(progressService.getChapterBookmarks(user.userId(), licenseClass, chapterId));
    }

    @PutMapping({
            "/api/v1/progress/chapters/{chapterId}/bookmarks",
            "/api/v1/learning/chapters/{chapterId}/bookmarks",
            "/api/v1/progress/chapters/{chapterId}/bookmarks/{questionId}",
            "/api/v1/learning/chapters/{chapterId}/bookmarks/{questionId}"
    })
    @PreAuthorize("hasAuthority('PERM_progress:update')")
    @Operation(summary = "Đánh dấu hoặc bỏ đánh dấu câu hỏi")
    public ApiResponse<BookmarkResponse> setBookmark(
            @AuthenticationPrincipal JwtUserDetails user,
            @PathVariable int chapterId,
            @PathVariable(required = false) Integer questionId,
            @RequestBody Map<String, Object> body
    ) {
        String license = body.containsKey("licenseClass") ? String.valueOf(body.get("licenseClass")) : "B2";
        int targetQuestionId = questionId != null ? questionId : ((Number) body.get("questionId")).intValue();
        boolean bookmarked = Boolean.TRUE.equals(body.get("bookmarked"));
        return ApiResponse.ok(progressService.setBookmark(user.userId(), license, chapterId, targetQuestionId, bookmarked));
    }

    // 4. Critical progress
    @GetMapping({
            "/api/v1/progress/critical",
            "/api/v1/questions/critical",
            "/api/v1/questions/critical/overview",
            "/api/v1/progress/critical/overview"
    })
    @PreAuthorize("hasAuthority('PERM_progress:read')")
    @Operation(summary = "Lấy danh sách và tiến độ 60 câu điểm liệt")
    public ApiResponse<CriticalOverview> getCriticalOverview(
            @AuthenticationPrincipal JwtUserDetails user,
            @RequestParam(defaultValue = "B2") String licenseClass
    ) {
        return ApiResponse.ok(progressService.getCriticalOverview(user.userId(), licenseClass));
    }

    // 5. Wrong questions
    @GetMapping({
            "/api/v1/progress/wrong/overview",
            "/api/v1/questions/wrong/overview"
    })
    @PreAuthorize("hasAuthority('PERM_progress:read')")
    @Operation(summary = "Lấy tổng quan các câu làm sai cần ôn lại")
    public ApiResponse<WrongOverview> getWrongOverview(
            @AuthenticationPrincipal JwtUserDetails user,
            @RequestParam(defaultValue = "B2") String licenseClass
    ) {
        return ApiResponse.ok(progressService.getWrongOverview(user.userId(), licenseClass));
    }

    @GetMapping({
            "/api/v1/progress/wrong",
            "/api/v1/questions/wrong"
    })
    @PreAuthorize("hasAuthority('PERM_progress:read')")
    @Operation(summary = "Lấy danh sách các câu làm sai cần ôn lại")
    public ApiResponse<List<WrongItem>> getWrongQuestionsList(
            @AuthenticationPrincipal JwtUserDetails user,
            @RequestParam(defaultValue = "B2") String licenseClass
    ) {
        return ApiResponse.ok(progressService.getWrongOverview(user.userId(), licenseClass).questions());
    }
}
