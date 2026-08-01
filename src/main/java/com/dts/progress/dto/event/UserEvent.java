package com.dts.progress.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserEvent(
        String eventType,
        UserPayload payload
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserPayload(
            UUID userId,
            String username,
            String email,
            String fullName
    ) {}
}
