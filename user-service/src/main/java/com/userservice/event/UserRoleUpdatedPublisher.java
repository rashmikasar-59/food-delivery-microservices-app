package com.userservice.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes role changes so the Auth Service's login database stays in sync. */
@Component
public class UserRoleUpdatedPublisher {
    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;
    private final String topic;

    public UserRoleUpdatedPublisher(
            KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate,
            @Value("${app.kafka.user-registered-topic:user-registered-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(UserRegisteredEvent event) {
        // Role-update event intentionally does not carry a password.
        kafkaTemplate.send(topic, event.getEmail(), event);
    }
}
