package com.fooddelivery.notification.consumer;

import com.fooddelivery.notification.event.UserRegisteredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserNotificationConsumerTest {

    @Mock
    private JavaMailSender mailSender;

    private UserNotificationConsumer consumer;

    private UserRegisteredEvent event;

    @BeforeEach
    void setUp() {
        consumer = new UserNotificationConsumer(
                mailSender, "noreply@fooddelivery.com");

        event = new UserRegisteredEvent();
        event.setUserId("user-101");
        event.setName("Test Customer");
        event.setEmail("customer@example.com");
        event.setRole("CUSTOMER");
    }

    @Test
    void consumeUserRegisteredEventShouldSendWelcomeEmail() {
        consumer.consumeUserRegisteredEvent(event);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage message = captor.getValue();

        assertEquals("noreply@fooddelivery.com", message.getFrom());
        assertArrayEquals(
                new String[]{"customer@example.com"},
                message.getTo());
        assertEquals("Welcome to Food Delivery!", message.getSubject());
        assertTrue(message.getText().contains("Hello Test Customer"));
        assertTrue(message.getText().contains(
                "Your account has been registered successfully."));
        assertTrue(message.getText().contains("Food Delivery Team"));
    }

    @Test
    void consumeUserRegisteredEventShouldSendOneEmailPerEvent() {
        consumer.consumeUserRegisteredEvent(event);

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void consumeUserRegisteredEventShouldUseEventRecipient() {
        event.setEmail("another@example.com");

        consumer.consumeUserRegisteredEvent(event);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        assertArrayEquals(
                new String[]{"another@example.com"},
                captor.getValue().getTo());
    }
}
