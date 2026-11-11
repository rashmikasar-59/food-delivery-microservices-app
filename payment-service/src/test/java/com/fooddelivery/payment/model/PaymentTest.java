package com.fooddelivery.payment.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    @Test
    void shouldCreatePaymentWithDefaultConstructor() {
        Payment payment = new Payment();

        assertNotNull(payment);
        assertNull(payment.getId());
        assertNull(payment.getOrderId());
        assertNull(payment.getCustomerId());
        assertEquals(0.0, payment.getAmount());
        assertNull(payment.getPaymentMethod());
        assertNull(payment.getStatus());
        assertNull(payment.getTransactionId());
        assertNull(payment.getCreatedAt());
        assertNull(payment.getUpdatedAt());
    }

    @Test
    void shouldSetAndGetAllPaymentFields() {
        Payment payment = new Payment();

        LocalDateTime createdAt = LocalDateTime.of(2026, 11, 10, 10, 30);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 11, 10, 11, 30);

        payment.setId("payment-101");
        payment.setOrderId("order-101");
        payment.setCustomerId("customer-101");
        payment.setAmount(1499.50);
        payment.setPaymentMethod("UPI");
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId("txn-101");
        payment.setCreatedAt(createdAt);
        payment.setUpdatedAt(updatedAt);

        assertEquals("payment-101", payment.getId());
        assertEquals("order-101", payment.getOrderId());
        assertEquals("customer-101", payment.getCustomerId());
        assertEquals(1499.50, payment.getAmount());
        assertEquals("UPI", payment.getPaymentMethod());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals("txn-101", payment.getTransactionId());
        assertEquals(createdAt, payment.getCreatedAt());
        assertEquals(updatedAt, payment.getUpdatedAt());
    }
}
