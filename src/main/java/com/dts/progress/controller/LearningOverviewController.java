package com.dts.progress.controller;

import com.dts.progress.dto.response.ApiResponse;
import com.dts.progress.dto.response.LearningOverviewResponse;
import com.dts.progress.security.JwtUserDetails;
import com.dts.progress.service.LearningOverviewService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/progress/learning-overview")
@SecurityRequirement(name = "BearerAuth")
@RequiredArgsConstructor
public class LearningOverviewController {

    private final LearningOverviewService service;

    @GetMapping
    public ApiResponse<LearningOverviewResponse> get(
            @AuthenticationPrincipal JwtUserDetails user,
            @RequestParam String licenseClass,
            @RequestHeader("Authorization") String authorization
    ) {
        return ApiResponse.ok(
                service.get(user.userId(), licenseClass, authorization)
        );
    }
}