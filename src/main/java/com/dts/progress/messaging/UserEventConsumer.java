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
        log.info("Received user event: type={}, userId={}", event.eventType(), event.userId());

        switch (event.eventType()) {
            case "USER_CREATED" -> handleUserCreated(event);
            case "USER_UPDATED" -> handleUserUpdated(event);
            case "USER_DELETED" -> handleUserDeleted(event);
            default -> log.debug("Ignored user event type: {}", event.eventType());
        }
    }

    private void handleUserCreated(UserEvent event) {
        if (event.userId() == null) {
            log.warn("User event without userId, skipping");
            return;
        }
        if (userProgressRepository.existsByUserId(event.userId())) {
            log.debug("UserProgress already exists for userId={}, skipping", event.userId());
            return;
        }
        UserProgress progress = UserProgress.builder()
                .userId(event.userId())
                .username(event.username() != null ? event.username() : "unknown")
                .build();
        userProgressRepository.save(progress);
        log.info("Created UserProgress for userId={}", event.userId());
    }

    private void handleUserUpdated(UserEvent event) {
        if (event.userId() == null) {
            log.warn("User event without userId, skipping");
            return;
        }
        Optional<UserProgress> existing = userProgressRepository.findByUserId(event.userId());
        if (existing.isPresent()) {
            UserProgress progress = existing.get();
            if (event.username() != null) {
                progress.setUsername(event.username());
            }
            userProgressRepository.save(progress);
            log.debug("Updated UserProgress for userId={}", event.userId());
        } else {
            // Create if not exists (event ordering edge case)
            handleUserCreated(event);
        }
    }

    private void handleUserDeleted(UserEvent event) {
        if (event.userId() == null) {
            log.warn("User event without userId, skipping");
            return;
        }
        userProgressRepository.findByUserId(event.userId())
                .ifPresent(progress -> {
                    userProgressRepository.delete(progress);
                    log.info("Deleted UserProgress for userId={}", event.userId());
                });
    }
}
