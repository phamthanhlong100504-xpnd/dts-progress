package com.dts.progress.messaging;

import com.dts.progress.dto.event.UserEvent;
import com.dts.progress.entity.UserProgress;
import com.dts.progress.repository.UserProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final UserProgressRepository userProgressRepository;

    @KafkaListener(topics = "${spring.kafka.topics.user-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserEvent(UserEvent event) {
        log.info("Received user event: type={}, userId={}", event.eventType(), event.payload().userId());

        switch (event.eventType()) {
            case "USER_CREATED" -> handleUserCreated(event);
            case "USER_UPDATED" -> handleUserUpdated(event);
            case "USER_DELETED" -> handleUserDeleted(event);
            default -> log.debug("Ignored user event type: {}", event.eventType());
        }
    }

    private void handleUserCreated(UserEvent event) {
        UserEvent.UserPayload payload = event.payload();
        if (userProgressRepository.existsByUserId(payload.userId())) {
            log.debug("UserProgress already exists for userId={}, skipping", payload.userId());
            return;
        }
        UserProgress progress = UserProgress.builder()
                .userId(payload.userId())
                .username(payload.username() != null ? payload.username() : "unknown")
                .build();
        userProgressRepository.save(progress);
        log.info("Created UserProgress for userId={}", payload.userId());
    }

    private void handleUserUpdated(UserEvent event) {
        UserEvent.UserPayload payload = event.payload();
        Optional<UserProgress> existing = userProgressRepository.findByUserId(payload.userId());
        if (existing.isPresent()) {
            UserProgress progress = existing.get();
            if (payload.username() != null) {
                progress.setUsername(payload.username());
            }
            userProgressRepository.save(progress);
            log.debug("Updated UserProgress for userId={}", payload.userId());
        } else {
            // Create if not exists (event ordering edge case)
            handleUserCreated(event);
        }
    }

    private void handleUserDeleted(UserEvent event) {
        UserEvent.UserPayload payload = event.payload();
        userProgressRepository.findByUserId(payload.userId())
                .ifPresent(progress -> {
                    userProgressRepository.delete(progress);
                    log.info("Deleted UserProgress for userId={}", payload.userId());
                });
    }
}
