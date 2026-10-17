package com.dts.progress.service;

import com.dts.progress.dto.event.PracticeExamFinishedEvent;
import com.dts.progress.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.UUID;

@Component
public class PracticeProgressReceiptClient {

    private final RestClient restClient;

    public PracticeProgressReceiptClient(
            RestClient.Builder builder,
            @Value("${practice.service-url:http://localhost:8087}")
            String practiceUrl
    ) {
        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);

        this.restClient = builder
                .baseUrl(practiceUrl)
                .requestFactory(factory)
                .build();
    }

    public PracticeExamFinishedEvent getCanonical(
            UUID userId,
            UUID examId,
            String authorization
    ) {
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            throw BusinessException.forbidden(
                    "Missing bearer token"
            );
        }

        try {
            ReceiptEnvelope response = restClient.get()
                    .uri(
                            "/api/v1/exams/{examId}/progress",
                            examId
                    )
                    .header(
                            "Authorization",
                            authorization
                    )
                    .retrieve()
                    .body(ReceiptEnvelope.class);

            if (response == null
                    || !response.success()
                    || response.data() == null
                    || !examId.equals(response.data().examId())) {
                throw BusinessException.of(
                        "PRACTICE_RESPONSE",
                        "Invalid Practice progress receipt",
                        HttpStatus.BAD_GATEWAY
                );
            }

            Receipt receipt = response.data();

            return new PracticeExamFinishedEvent(
                    examId,
                    "EXAM_FINISHED",
                    1,
                    "dts-practice",
                    userId,
                    examId,
                    receipt.examType(),
                    receipt.bankVersion(),
                    receipt.mode(),
                    receipt.status(),
                    receipt.totalQuestions(),
                    receipt.correctCount(),
                    receipt.wrongCount(),
                    receipt.answeredWrongCount(),
                    receipt.unansweredCount(),
                    receipt.score(),
                    receipt.passed(),
                    receipt.durationSeconds(),
                    receipt.startedAt(),
                    receipt.completedAt()
            );
        } catch (RestClientResponseException error) {
            int status = error.getStatusCode().value();

            if (status == 400 || status == 409) {
                throw BusinessException.conflict(
                        "Practice exam is not finished"
                );
            }

            if (status == 401 || status == 403) {
                throw BusinessException.forbidden(
                        "Cannot access Practice exam"
                );
            }

            if (status == 404) {
                throw BusinessException.notFound(
                        "Practice exam not found"
                );
            }

            throw BusinessException.of(
                    "PRACTICE_UNAVAILABLE",
                    "Practice service is unavailable",
                    HttpStatus.BAD_GATEWAY
            );
        } catch (RestClientException error) {
            throw BusinessException.of(
                    "PRACTICE_UNAVAILABLE",
                    "Practice service is unavailable",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReceiptEnvelope(
            boolean success,
            Receipt data
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Receipt(
            UUID examId,
            String examType,
            String bankVersion,
            String mode,
            String status,
            Integer totalQuestions,
            Integer correctCount,
            Integer wrongCount,
            Integer answeredWrongCount,
            Integer unansweredCount,
            Integer score,
            Boolean passed,
            Integer durationSeconds,
            Instant startedAt,
            Instant completedAt
    ) {}
}