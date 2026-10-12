package com.paymentservice.service;




import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.paymentservice.model.Payment;
import com.paymentservice.model.PaymentStatus;
import com.paymentservice.model.PaymentNotificationEvent;
import com.paymentservice.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, PaymentNotificationEvent> kafkaTemplate;
    @Value("${app.kafka.payment-notification-topic:payment-notification-topic}")
    private String paymentNotificationTopic;
    public String processPayment(Payment payment, String customerUsername, String customerRole, String authorization) {
        if (payment == null || payment.getOrderId() == null || payment.getOrderId().isBlank()) {
            throw new IllegalArgumentException("orderId is required for payment");
        }
        if (payment.getAmount() == null || !Double.isFinite(payment.getAmount()) || payment.getAmount() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (paymentRepository.existsByOrderId(payment.getOrderId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "A payment has already been recorded for this order");
        }
        // Validate order ownership and expected amount before accepting a demo payment.
        org.springframework.web.reactive.function.client.WebClient client = webClientBuilder.build();
        java.util.Map<?, ?> order = client.get()
                .uri("http://order-service/api/v1/order/{orderId}", payment.getOrderId())
                .header("loggedInUser", customerUsername)
                .header("loggedInRole", customerRole)
                .header("Authorization", authorization)
                .retrieve().bodyToMono(java.util.Map.class).block();
        if (order == null || order.get("totalAmount") == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Order not found or amount unavailable");
        }
        if (!"PENDING".equals(String.valueOf(order.get("orderStatus")))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Only pending orders can be paid");
        }
        double expectedAmount;
        try { expectedAmount = Double.parseDouble(order.get("totalAmount").toString()); }
        catch (NumberFormatException ex) { throw new IllegalStateException("Order amount is invalid", ex); }
        if (Math.abs(expectedAmount - payment.getAmount()) > 0.01d) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Payment amount does not match order total");
        }
        // DEMO ONLY: this marks payment approved without a real payment provider.
        // Replace with a provider integration before production use.
        payment.setTimestamp(System.currentTimeMillis());
        payment.setPaymentStatus(PaymentStatus.APPROVED);
        payment = paymentRepository.save(payment);

        // Typed, versionable event; order-service validates order ownership before applying it.
        kafkaTemplate.send(paymentNotificationTopic, payment.getOrderId(),
                new PaymentNotificationEvent(payment.getId(), payment.getOrderId(), "SUCCESS", customerUsername));

        return "payment was " + payment.getPaymentStatus();
    }
}
