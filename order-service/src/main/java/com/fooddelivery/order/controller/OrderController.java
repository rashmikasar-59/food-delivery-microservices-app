package com.fooddelivery.order.controller;

import com.fooddelivery.order.dto.CreateOrderRequest;
import com.fooddelivery.order.dto.UpdateOrderStatusRequest;
import com.fooddelivery.order.model.Order;
import com.fooddelivery.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request) {

        Order order = orderService.createOrder(
                authentication.getName(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/customer")
    public ResponseEntity<List<Order>> getMyOrders(
            Authentication authentication) {

        return ResponseEntity.ok(
                orderService.getOrdersByCustomer(authentication.getName()));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(
            @PathVariable String orderId,
            Authentication authentication) {

        return ResponseEntity.ok(
                orderService.getOrderForCustomer(
                        orderId, authentication.getName()));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                orderService.updateOrderStatus(
                        orderId,
                        request.getStatus(),
                        authentication.getName(),
                        authentication.getAuthorities()));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(
            @PathVariable String orderId,
            Authentication authentication) {

        orderService.cancelOrder(orderId, authentication.getName());

        return ResponseEntity.ok("Order cancelled successfully");
    }
}
