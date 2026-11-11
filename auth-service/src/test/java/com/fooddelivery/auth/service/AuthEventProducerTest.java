package com.fooddelivery.auth.service;

import com.fooddelivery.auth.event.UserRegisteredEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthEventProducerTest {

    @Mock
    private KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    @InjectMocks
    private AuthEventProducer authEventProducer;

    @Test
    void publishUserRegisteredEventShouldSendEventToKafka() {
        UserRegisteredEvent event = new UserRegisteredEvent(
                "user-123",
                "Test User",
                "test@example.com",
                "CUSTOMER",
                LocalDateTime.now());

        CompletableFuture<SendResult<String, UserRegisteredEvent>> future =
                new CompletableFuture<>();

        when(kafkaTemplate.send(
                "user.registered", "user-123", event))
                .thenReturn(future);

        authEventProducer.publishUserRegisteredEvent(event);

        verify(kafkaTemplate).send(
                "user.registered", "user-123", event);

        future.completeExceptionally(new RuntimeException("Test completion"));
    }

    @Test
    void publishUserRegisteredEventShouldHandleKafkaFailure() {
        UserRegisteredEvent event = new UserRegisteredEvent(
                "user-456",
                "Another User",
                "another@example.com",
                "CUSTOMER",
                LocalDateTime.now());

        CompletableFuture<SendResult<String, UserRegisteredEvent>> failedFuture =
                CompletableFuture.failedFuture(
                        new RuntimeException("Kafka unavailable"));

        when(kafkaTemplate.send(
                "user.registered", "user-456", event))
                .thenReturn(failedFuture);

        assertDoesNotThrow(
                () -> authEventProducer.publishUserRegisteredEvent(event));

        verify(kafkaTemplate).send(
                "user.registered", "user-456", event);
    }
}
