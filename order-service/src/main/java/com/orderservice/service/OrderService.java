package com.orderservice.service;


import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.math.RoundingMode;
import org.springframework.core.ParameterizedTypeReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.orderservice.dto.OrderItemDto;
import com.orderservice.dto.OrderResponse;
import com.orderservice.event.OrderPlacedNotification;
import com.orderservice.event.PaymentNotificationEvent;
import com.orderservice.model.Order;
import com.orderservice.model.OrderItem;
import com.orderservice.model.OrderStatus;
import com.orderservice.repository.OrderRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedNotification> kafkaTemplate;
    @Value("${app.kafka.order-rest-notification-topic:order-rest-notification-topic}")
    private String restaurantNotificationTopic;
    public OrderResponse createOrder(Order order) {
        if (order == null || order.getRestaurantId() == null || order.getRestaurantId().isBlank()
                || order.getOrderItems() == null || order.getOrderItems().isEmpty() || order.getAddress() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Restaurant, address and at least one order item are required");
        }
        // Capture the authoritative restaurant owner so later status changes can be authorized.
        Map<?, ?> restaurantOwner = webClientBuilder.build().get()
                .uri("http://restaurant-service/api/v1/restaurant/internal/{restaurantId}/owner", order.getRestaurantId())
                .header("X-Internal-Service-Key", internalServiceKey())
                .retrieve().bodyToMono(Map.class).block();
        if (restaurantOwner == null || restaurantOwner.get("username") == null) {
            throw new IllegalStateException("Restaurant owner could not be verified");
        }
        order.setRestaurantOwnerUsername(restaurantOwner.get("username").toString());
        // Never trust price/name/restaurant identifiers supplied by a client. Read current menu data.
        List<Map<String, Object>> menuItems = webClientBuilder.build().get()
                .uri("http://restaurant-service/api/v1/fooditem/{restaurantId}", order.getRestaurantId())
                .retrieve().bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .block();
        if (menuItems == null || menuItems.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "No menu items found for restaurant");
        }
        Map<String, Map<String, Object>> menuById = new HashMap<>();
        for (Map<String, Object> item : menuItems) {
            Object id = item.get("id");
            if (id != null) menuById.put(id.toString(), item);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getOrderItems()) {
            if (item == null || item.getFoodItemId() == null || item.getQuantity() == null || item.getQuantity() < 1) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Each order item requires a valid foodItemId and positive quantity");
            }
            Map<String, Object> menu = menuById.get(item.getFoodItemId());
            if (menu == null) throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Food item does not belong to the selected restaurant");
            int available = menu.get("quantity") instanceof Number n ? n.intValue() : 0;
            if (available < item.getQuantity()) throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Insufficient stock for " + menu.get("name"));
            BigDecimal price;
            try { price = new BigDecimal(menu.get("price").toString()); }
            catch (RuntimeException ex) { throw new IllegalStateException("Menu item has an invalid price", ex); }
            if (price.signum() < 0) throw new IllegalStateException("Menu item price cannot be negative");
            item.setName(String.valueOf(menu.get("name")));
            item.setPrice(price);
            total = total.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        order.setOrderTime(System.currentTimeMillis());
        order.setOrderStatus(OrderStatus.PENDING);

        List<String> foodItemIds = order.getOrderItems().stream().map(OrderItem::getFoodItemId).toList();
        List<Integer> orderQuantities = order.getOrderItems().stream().map(OrderItem::getQuantity).toList();
        // Reserve inventory first; if the service rejects the reservation, no order is persisted.
        updateInventory(foodItemIds, orderQuantities);
        order = orderRepository.save(order);

        return OrderResponse.builder()
                .id(order.getId())
                .orderItems(order.getOrderItems().stream().map(orderItem -> OrderItemDto.builder()
                        .name(orderItem.getName())
                        .price(orderItem.getPrice())
                        .quantity(orderItem.getQuantity())
                        .build()).toList())
                .totalAmount(order.getTotalAmount())
                .orderTime(order.getOrderTime())
                .orderStatus(order.getOrderStatus())
                .address(order.getAddress())
                .build();
    }

    @CircuitBreaker(name = "downstreamService", fallbackMethod = "inventoryFallback")
    @Retry(name = "downstreamService")
    public void updateInventory(List<String> foodItemIds, List<Integer> orderQuantities) {
        webClientBuilder.build().put()
                .uri("http://restaurant-service/api/v1/fooditem/quantity",
                        uriBuilder -> uriBuilder.queryParam("foodItemIds", foodItemIds)
                                .queryParam("orderQuantities", orderQuantities).build())
                .header("X-Internal-Service-Key", internalServiceKey())
                .retrieve().toBodilessEntity().block();
    }

    private String internalServiceKey() {
        String key = System.getenv("INTERNAL_SERVICE_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INTERNAL_SERVICE_KEY is not configured");
        return key;
    }

    private void inventoryFallback(List<String> foodItemIds, List<Integer> orderQuantities, Throwable cause) {
        throw new IllegalStateException("Inventory service is unavailable; order cannot be completed safely", cause);
    }

    public void updateOrderAfterPayment(PaymentNotificationEvent event) {
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found for payment event: " + event.getOrderId()));
        // The payment event must belong to the order owner. Kafka delivery is at-least-once.
        if (event.getCustomerUsername() == null || order.getUserId() == null
                || !order.getUserId().equalsIgnoreCase(event.getCustomerUsername())) {
            throw new IllegalArgumentException("Payment event customer does not own this order");
        }
        if (order.getPaymentId() != null) return;
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Payment event cannot be applied because order is not pending");
        }
        if ("SUCCESS".equalsIgnoreCase(event.getStatus())) {
            order.setPaymentId(event.getPaymentId());
            order.setOrderStatus(OrderStatus.CONFIRMED);
            order.setDeliveryTime(order.getOrderTime() + 30 * 60 * 1000L);
            orderRepository.save(order);
            sendNotificationToRestaurant(order);
        } else {
            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }
    }

    private void sendNotificationToRestaurant(Order order) {
        // Send a notification to Restaurant Service to prepare this order
        OrderPlacedNotification notification = OrderPlacedNotification.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .orderAddress(order.getAddress())
                .foodItemIds(order.getOrderItems().stream()
                        .map(OrderItem::getFoodItemId).toList())
                .foodItemQuantities(order.getOrderItems().stream()
                        .map(OrderItem::getQuantity).toList())
                .build();
        kafkaTemplate.send(restaurantNotificationTopic, order.getId(), notification);
    }

    public Order getOrderForCustomer(String orderId, String username, String role) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Order not found"));
        if (!"ADMIN".equals(role) && (order.getUserId() == null || !order.getUserId().equalsIgnoreCase(username))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "You may only access your own order");
        }
        return order;
    }

    public void updateOrderStatus(String orderId, OrderStatus orderStatus, String username, String role) {
        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        if(optionalOrder.isPresent()) {
            Order order = optionalOrder.get();
            if ("CUSTOMER".equals(role) && (order.getUserId() == null || !order.getUserId().equalsIgnoreCase(username) || orderStatus != OrderStatus.CANCELLED)) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Customers may only cancel their own order");
            }
            if ("RESTAURANT_OWNER".equals(role) && (order.getRestaurantOwnerUsername() == null
                    || !order.getRestaurantOwnerUsername().equalsIgnoreCase(username))) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "You do not own the restaurant for this order");
            }
            if ("DELIVERY_AGENT".equals(role) && (order.getDeliveryAgentUsername() == null
                    || !order.getDeliveryAgentUsername().equalsIgnoreCase(username))) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "This order is not assigned to you");
            }
            if (!isAllowedTransition(order.getOrderStatus(), orderStatus)) {
                throw new IllegalArgumentException("Invalid order status transition: " + order.getOrderStatus() + " -> " + orderStatus);
            }
            order.setOrderStatus(orderStatus);
            orderRepository.save(order);
        } else {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
    }

    public Order assignDeliveryAgent(String orderId, String agentUsername) {
        if (agentUsername == null || agentUsername.isBlank()) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Delivery agent username is required");
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getOrderStatus() != OrderStatus.PREPARING
                && order.getOrderStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Order must be confirmed/preparing before assignment");
        }
        order.setDeliveryAgentUsername(agentUsername.trim().toLowerCase(java.util.Locale.ROOT));
        if (order.getOrderStatus() == OrderStatus.PREPARING) order.setOrderStatus(OrderStatus.OUT_FOR_DELIVERY);
        return orderRepository.save(order);
    }

    private boolean isAllowedTransition(OrderStatus current, OrderStatus next) {
        if (current == null || next == null || current == next) return false;
        return switch (current) {
            case PENDING -> next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.PREPARING;
            case PREPARING -> next == OrderStatus.OUT_FOR_DELIVERY || next == OrderStatus.DELIVERING;
            case OUT_FOR_DELIVERY, DELIVERING -> next == OrderStatus.DELIVERED || next == OrderStatus.COMPLETED;
            default -> false;
        };
    }
}
