package com.dts.progress.controller;

import com.dts.progress.dto.event.PracticeExamFinishedEvent;
import com.dts.progress.dto.response.ApiResponse;
import com.dts.progress.dto.response.SessionHistoryResponse;
import com.dts.progress.exception.BusinessException;
import com.dts.progress.service.CanonicalExamProgressService;
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

@RestController
@RequestMapping("/internal/v1/practice")
@RequiredArgsConstructor
public class PracticeExamProgressIngressController {

    private final CanonicalExamProgressService canonicalService;
    private final ObjectMapper objectMapper;

    @Value("${progress.internal-key:}")
    private String internalKey;

    @PostMapping("/exam-results")
    public ApiResponse<SessionHistoryResponse> receive(
            @RequestHeader(
                    value = "X-DTS-Internal-Key",
                    required = false
            )
            String providedKey,
            @RequestBody String body
    ) {
        if (internalKey == null
                || internalKey.isBlank()
                || providedKey == null
                || !MessageDigest.isEqual(
                        internalKey.getBytes(
                                StandardCharsets.UTF_8
                        ),
                        providedKey.getBytes(
                                StandardCharsets.UTF_8
                        )
                )) {
            throw BusinessException.of(
                    "INTERNAL_AUTH_REQUIRED",
                    "Internal authentication required",
                    HttpStatus.FORBIDDEN
            );
        }

        PracticeExamFinishedEvent event;

        try {
            event = objectMapper.readValue(
                    body,
                    PracticeExamFinishedEvent.class
            );
        } catch (JsonProcessingException error) {
            throw BusinessException.badRequest(
                    "Invalid exam progress event"
            );
        }

        return ApiResponse.ok(
                canonicalService.apply(event)
        );
    }
}