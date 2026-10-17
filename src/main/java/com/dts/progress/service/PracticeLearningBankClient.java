package com.dts.progress.service;

import com.dts.progress.dto.response.LearningBankCatalogResponse;
import com.dts.progress.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class PracticeLearningBankClient {

    private final RestClient client;

    public PracticeLearningBankClient(
            RestClient.Builder builder,
            @Value("${practice.service-url:http://localhost:8087}") String practiceUrl
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        this.client = builder.baseUrl(practiceUrl)
                .requestFactory(factory).build();
    }

    public LearningBankCatalogResponse get(String licenseClass, String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw BusinessException.forbidden("Missing bearer token");
        }
        try {
            Envelope response = client.get()
                    .uri(uri -> uri.path("/api/v1/questions/learning-catalog")
                            .queryParam("licenseClass", licenseClass).build())
                    .header("Authorization", authorization)
                    .retrieve()
                    .body(Envelope.class);

            if (response == null || !response.success() || response.data() == null
                    || !licenseClass.equals(response.data().licenseClass())
                    || response.data().bankVersion() == null
                    || response.data().bankVersion().isBlank()
                    || response.data().totalQuestions() <= 0
                    || response.data().chapters() == null) {
                throw BusinessException.of("PRACTICE_RESPONSE",
                        "Invalid Practice learning catalog", HttpStatus.BAD_GATEWAY);
            }
            return response.data();
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 400) {
                throw BusinessException.badRequest("Invalid license class");
            }
            if (error.getStatusCode().value() == 401
                    || error.getStatusCode().value() == 403) {
                throw BusinessException.forbidden("Cannot access Practice catalog");
            }
            throw BusinessException.of("PRACTICE_UNAVAILABLE",
                    "Practice question bank is unavailable", HttpStatus.BAD_GATEWAY);
        } catch (RestClientException error) {
            throw BusinessException.of("PRACTICE_UNAVAILABLE",
                    "Practice question bank is unavailable", HttpStatus.BAD_GATEWAY);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Envelope(boolean success, LearningBankCatalogResponse data) {}
}
