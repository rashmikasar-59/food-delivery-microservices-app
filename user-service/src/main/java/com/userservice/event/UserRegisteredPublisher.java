package com.userservice.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserRegisteredPublisher {
    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;
    private final String topic;
    public UserRegisteredPublisher(KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate,
            @Value("${app.kafka.user-registered-topic:user-registered-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate; this.topic = topic;
    }
    public void publish(UserRegisteredEvent event) {
        kafkaTemplate.send(topic, event.getEmail(), event);
    }
}
