
package com.fooddelivery.auth.service;

import com.fooddelivery.auth.event.UserRegisteredEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthEventProducer {

    private static final String TOPIC = "user.registered";

    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    public AuthEventProducer(
            KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserRegisteredEvent(UserRegisteredEvent event) {

        kafkaTemplate.send(
                TOPIC,
                event.getUserId(),
                event
        ).whenComplete((result, exception) -> {

            if (exception != null) {
                System.err.println(
                        "Failed to publish UserRegisteredEvent: "
                                + exception.getMessage()
                );
            } else {
                System.out.println(
                        "UserRegisteredEvent published successfully. Topic: "
                                + result.getRecordMetadata().topic()
                );
            }
        });
    }
}
