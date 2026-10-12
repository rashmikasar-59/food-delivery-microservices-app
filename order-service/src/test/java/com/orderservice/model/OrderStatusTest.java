package com.orderservice.model;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.orderservice.model.OrderStatus;

class OrderStatusTest {
    @Test void workflowContainsExpectedOperationalStates() {
        assertTrue(java.util.Arrays.asList(OrderStatus.values()).containsAll(java.util.List.of(
                OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING,
                OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED, OrderStatus.CANCELLED)));
    }
    @Test void terminalStatusesExist() {
        assertNotNull(OrderStatus.valueOf("DELIVERED"));
        assertNotNull(OrderStatus.valueOf("CANCELLED"));
    }
}
