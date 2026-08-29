package com.dts.progress.messaging;

import com.dts.progress.dto.event.UserEvent;
import com.dts.progress.entity.UserProgress;
import com.dts.progress.repository.UserProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventConsumerTest {

    @Mock
    private UserProgressRepository userProgressRepository;

    @InjectMocks
    private UserEventConsumer userEventConsumer;

    private UUID userId;
    private String username;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        username = "testuser";
    }

    @Test
    @DisplayName("USER_CREATED - Creates UserProgress when not exists")
    void testHandleUserCreated_CreatesProgress() {
        UserEvent event = new UserEvent("USER_CREATED", userId, username, "test@example.com", "Test User");
        when(userProgressRepository.existsByUserId(userId)).thenReturn(false);

        userEventConsumer.handleUserEvent(event);

        verify(userProgressRepository).save(argThat(progress ->
                progress.getUserId().equals(userId) &&
                progress.getUsername().equals(username)
        ));
    }

    @Test
    @DisplayName("USER_CREATED - Skips when UserProgress already exists")
    void testHandleUserCreated_AlreadyExists() {
        UserEvent event = new UserEvent("USER_CREATED", userId, username, "test@example.com", "Test User");
        when(userProgressRepository.existsByUserId(userId)).thenReturn(true);

        userEventConsumer.handleUserEvent(event);

        verify(userProgressRepository, never()).save(any());
    }

    @Test
    @DisplayName("USER_CREATED - Handles null userId gracefully")
    void testHandleUserCreated_NullUserId() {
        UserEvent event = new UserEvent("USER_CREATED", null, username, "test@example.com", "Test User");

        userEventConsumer.handleUserEvent(event);

        verify(userProgressRepository, never()).save(any());
        verify(userProgressRepository, never()).existsByUserId(any());
    }

    @Test
    @DisplayName("USER_UPDATED - Updates username when progress exists")
    void testHandleUserUpdated_UpdatesUsername() {
        String newUsername = "updateduser";
        UserEvent event = new UserEvent("USER_UPDATED", userId, newUsername, "test@example.com", "Test User");

        UserProgress existingProgress = new UserProgress();
        existingProgress.setUserId(userId);
        existingProgress.setUsername(username);

        when(userProgressRepository.findByUserId(userId)).thenReturn(Optional.of(existingProgress));

        userEventConsumer.handleUserEvent(event);

        assertEquals(newUsername, existingProgress.getUsername());
        verify(userProgressRepository).save(existingProgress);
    }

    @Test
    @DisplayName("USER_UPDATED - Creates progress if not exists (edge case)")
    void testHandleUserUpdated_NotExists_Creates() {
        String newUsername = "updateduser";
        UserEvent event = new UserEvent("USER_UPDATED", userId, newUsername, "test@example.com", "Test User");

        when(userProgressRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userProgressRepository.existsByUserId(userId)).thenReturn(false);

        userEventConsumer.handleUserEvent(event);

        verify(userProgressRepository).save(argThat(progress ->
                progress.getUserId().equals(userId) &&
                progress.getUsername().equals(newUsername)
        ));
    }

    @Test
    @DisplayName("USER_UPDATED - Handles null userId gracefully")
    void testHandleUserUpdated_NullUserId() {
        UserEvent event = new UserEvent("USER_UPDATED", null, username, "test@example.com", "Test User");

        userEventConsumer.handleUserEvent(event);

        verify(userProgressRepository, never()).save(any());
        verify(userProgressRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("USER_DELETED - Deletes UserProgress when exists")
    void testHandleUserDeleted_DeletesProgress() {
        UserEvent event = new UserEvent("USER_DELETED", userId, username, "test@example.com", "Test User");

        UserProgress existingProgress = new UserProgress();
        existingProgress.setUserId(userId);

        when(userProgressRepository.findByUserId(userId)).thenReturn(Optional.of(existingProgress));

        userEventConsumer.handleUserEvent(event);

        verify(userProgressRepository).delete(existingProgress);
    }

    @Test
    @DisplayName("USER_DELETED - No-op when UserProgress not found")
    void testHandleUserDeleted_NotFound() {
        UserEvent event = new UserEvent("USER_DELETED", userId, username, "test@example.com", "Test User");

        when(userProgressRepository.findByUserId(userId)).thenReturn(Optional.empty());

        userEventConsumer.handleUserEvent(event);

        verify(userProgressRepository, never()).delete(any());
    }

    @Test
    @DisplayName("USER_DELETED - Handles null userId gracefully")
    void testHandleUserDeleted_NullUserId() {
        UserEvent event = new UserEvent("USER_DELETED", null, username, "test@example.com", "Test User");

        userEventConsumer.handleUserEvent(event);

        verify(userProgressRepository, never()).delete(any());
        verify(userProgressRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("Unknown event type - Logs debug and does nothing")
    void testHandleUserEvent_UnknownType() {
        UserEvent event = new UserEvent("UNKNOWN_EVENT", userId, username, "test@example.com", "Test User");

        userEventConsumer.handleUserEvent(event);

        verify(userProgressRepository, never()).save(any());
        verify(userProgressRepository, never()).delete(any());
        verify(userProgressRepository, never()).findByUserId(any());
        verify(userProgressRepository, never()).existsByUserId(any());
    }
}