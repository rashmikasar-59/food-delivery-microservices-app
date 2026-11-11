package com.fooddelivery.notification.consumer;

import com.fooddelivery.notification.event.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class UserNotificationConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(UserNotificationConsumer.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public UserNotificationConsumer(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @KafkaListener(
            topics = "user.registered",
            groupId = "notification-service-group"
    )
    public void consumeUserRegisteredEvent(UserRegisteredEvent event) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(event.getEmail());
        message.setSubject("Welcome to Food Delivery!");
        message.setText(
                "Hello " + event.getName() + ",\n\n" +
                "Welcome to Food Delivery!\n\n" +
                "Your account has been registered successfully.\n" +
                "We're happy to have you with us.\n\n" +
                "Thank you,\nFood Delivery Team"
        );

        mailSender.send(message);

        log.info("Welcome email sent successfully to {}", event.getEmail());
    }
}
