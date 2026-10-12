package com.orderservice.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.orderservice.dto.OrderResponse;
import com.orderservice.model.Order;
import com.orderservice.model.OrderStatus;
import com.orderservice.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/order")
@RequiredArgsConstructor
public class OrderController {
    @Autowired
    private OrderService orderService;
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@Valid @RequestBody Order order,
                                    @RequestHeader("loggedInUser") String username,
                                    @RequestHeader("loggedInRole") String role) {
        if (!"CUSTOMER".equals(role) && !"ADMIN".equals(role)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only customers can place orders");
        }
        order.setUserId(username);
        return orderService.createOrder(order);
    }

    @GetMapping("/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    public Order getOrder(@PathVariable String orderId,
                          @RequestHeader("loggedInUser") String username,
                          @RequestHeader("loggedInRole") String role) {
        return orderService.getOrderForCustomer(orderId, username, role);
    }

    @PatchMapping("/{orderId}/delivery-agent")
    @ResponseStatus(HttpStatus.OK)
    public Order assignDeliveryAgent(@PathVariable String orderId, @RequestParam String username,
                                     @RequestHeader("loggedInRole") String role) {
        if (!"ADMIN".equals(role)) throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.FORBIDDEN, "Only admins can assign delivery agents");
        return orderService.assignDeliveryAgent(orderId, username);
    }

    @PutMapping("/status")
    @ResponseStatus(HttpStatus.OK)
    public void updateOrderStatus(@RequestParam String orderId, @RequestBody OrderStatus orderStatus,
                                  @RequestHeader("loggedInUser") String username,
                                  @RequestHeader(value = "loggedInRole", defaultValue = "CUSTOMER") String role) {
        boolean allowed = switch (orderStatus) {
            case PREPARING -> role.equals("RESTAURANT_OWNER") || role.equals("ADMIN");
            case CONFIRMED -> role.equals("ADMIN");
            case OUT_FOR_DELIVERY, DELIVERING, DELIVERED -> role.equals("DELIVERY_AGENT") || role.equals("ADMIN");
            case CANCELLED -> role.equals("CUSTOMER") || role.equals("ADMIN");
            case COMPLETED -> role.equals("ADMIN");
            default -> role.equals("ADMIN");
        };
        if (!allowed) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "Role is not allowed to set this order status");
        orderService.updateOrderStatus(orderId, orderStatus, username, role);
    }
}
