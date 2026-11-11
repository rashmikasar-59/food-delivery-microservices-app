package com.fooddelivery.user.service;

import com.fooddelivery.user.entity.UserProfile;
import com.fooddelivery.user.event.UserRegisteredEvent;
import com.fooddelivery.user.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventConsumerTest {

    @Mock
    UserProfileRepository repository;

    @InjectMocks
    UserEventConsumer consumer;

    private UserRegisteredEvent createEvent() {
        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setUserId("user-123");
        event.setName("Test User");
        event.setEmail("test@example.com");
        event.setRole("CUSTOMER");
        event.setRegisteredAt(LocalDateTime.of(2026, 11, 10, 10, 0));
        return event;
    }

    @Test
    void consumeEventShouldCreateUserProfile() {
        UserRegisteredEvent event = createEvent();

        when(repository.existsByUserId("user-123")).thenReturn(false);

        when(repository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> consumer.consumeUserRegisteredEvent(event));

        verify(repository).existsByUserId("user-123");

        verify(repository).save(argThat((UserProfile profile) ->
                profile.getUserId().equals("user-123")
                && profile.getName().equals("Test User")
                && profile.getEmail().equals("test@example.com")
                && profile.getRole().equals("CUSTOMER")
                && profile.getRegisteredAt().equals(event.getRegisteredAt())));
    }

    @Test
    void consumeEventShouldSkipExistingUserProfile() {
        UserRegisteredEvent event = createEvent();

        when(repository.existsByUserId("user-123")).thenReturn(true);

        assertDoesNotThrow(() -> consumer.consumeUserRegisteredEvent(event));

        verify(repository).existsByUserId("user-123");
        verify(repository, never()).save(any(UserProfile.class));
    }
}
