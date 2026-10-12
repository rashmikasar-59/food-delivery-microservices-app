package com.orderservice.service;

import com.orderservice.event.PaymentNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentNotificationListener {
    private final OrderService orderService;
    @KafkaListener(topics = "${app.kafka.payment-notification-topic:payment-notification-topic}",
                   groupId = "${spring.kafka.consumer.group-id:order-payment-sync}")
    public void onPaymentNotification(PaymentNotificationEvent event) {
        if (event == null || event.getOrderId() == null || event.getPaymentId() == null || event.getStatus() == null) {
            throw new IllegalArgumentException("Invalid payment notification event");
        }
        log.info("Payment event received: orderId={}, paymentId={}, status={}", event.getOrderId(), event.getPaymentId(), event.getStatus());
        orderService.updateOrderAfterPayment(event);
    }
}
