package com.restaurantservice.listener;


import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.restaurantservice.utils.OrderPlacedNotification;

import lombok.extern.slf4j.Slf4j;

/**
 * Consumes order notifications published by Order Service after payment succeeds.
 * This listener currently logs the event so the Kafka integration can be verified.
 * Restaurant preparation/notification business logic can be added here.
 */
@Component
@Slf4j
public class OrderNotificationListener {

    @KafkaListener(
            topics = "${app.kafka.order-rest-notification-topic:order-rest-notification-topic}",
            groupId = "order-notification-id"
    )
    public void consumeOrderEvent(OrderPlacedNotification event) {
        if (event == null) {
            log.warn("Received an empty order notification from Kafka");
            return;
        }

        log.info("Kafka order notification received. orderId={}, userId={}, foodItemIds={}, quantities={}, address={}",
                event.getOrderId(),
                event.getUserId(),
                event.getFoodItemIds(),
                event.getFoodItemQuantities(),
                event.getOrderAddress());
    }
}
