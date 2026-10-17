package com.dts.progress.controller;

import com.dts.progress.dto.event.PracticeQuestionProgressEvent;
import com.dts.progress.dto.response.ApiResponse;
import com.dts.progress.exception.BusinessException;
import com.dts.progress.service.QuestionLearningProjectionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/internal/v1/practice")
@RequiredArgsConstructor
public class PracticeQuestionProgressIngressController {

    private final QuestionLearningProjectionService projectionService;
    private final ObjectMapper objectMapper;

    @Value("${progress.internal-key:}")
    private String internalKey;

    @PostMapping("/question-progress")
    public ApiResponse<Map<String, Boolean>> receive(
            @RequestHeader(value = "X-DTS-Internal-Key", required = false)
            String providedKey,
            @RequestBody String body
    ) {
        if (internalKey == null || internalKey.isBlank()
                || providedKey == null
                || !MessageDigest.isEqual(
                        internalKey.getBytes(StandardCharsets.UTF_8),
                        providedKey.getBytes(StandardCharsets.UTF_8))) {
            throw BusinessException.of(
                    "INTERNAL_AUTH_REQUIRED", "Internal authentication required",
                    HttpStatus.FORBIDDEN);
        }

        PracticeQuestionProgressEvent event;
        try {
            event = objectMapper.readValue(body, PracticeQuestionProgressEvent.class);
        } catch (JsonProcessingException error) {
            throw BusinessException.badRequest("Invalid question progress event");
        }
        projectionService.apply(event);
        return ApiResponse.ok(Map.of("accepted", true));
    }
}
