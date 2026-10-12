package com.deliveryservice.controller;

import com.deliveryservice.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    @PostMapping("/{orderId}")
    @ResponseStatus(HttpStatus.CREATED)
    public String deliverOrder(@PathVariable String orderId,
                               @RequestHeader("loggedInRole") String role,
                               @RequestHeader("loggedInUser") String username,
                               @RequestHeader("Authorization") String authorization) {
        if (!"DELIVERY_AGENT".equals(role) && !"ADMIN".equals(role)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only delivery agents or admins can complete deliveries");
        }
        return deliveryService.deliverOrder(orderId, username, authorization);
    }

    public void callCustomer() {
        deliveryService.callCustomer();
    }
}
