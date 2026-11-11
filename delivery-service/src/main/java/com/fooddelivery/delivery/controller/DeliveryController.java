package com.fooddelivery.delivery.controller;

import com.fooddelivery.delivery.dto.CreateDeliveryRequest;
import com.fooddelivery.delivery.dto.UpdateDeliveryStatusRequest;
import com.fooddelivery.delivery.model.Delivery;
import com.fooddelivery.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Delivery createDelivery(
            @Valid @RequestBody CreateDeliveryRequest request) {
        return deliveryService.createDelivery(request);
    }

    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<Delivery> getCustomerDeliveries(
            Authentication authentication) {
        return deliveryService.getCustomerDeliveries(authentication.getName());
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public List<Delivery> getAssignedDeliveries(
            Authentication authentication) {
        return deliveryService.getAssignedDeliveries(authentication.getName());
    }

    @GetMapping("/{deliveryId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'DELIVERY_PARTNER')")
    public Delivery getDeliveryById(
            @PathVariable String deliveryId,
            Authentication authentication) {
        return deliveryService.getDeliveryById(
                deliveryId, authentication.getName());
    }

    @PatchMapping("/{deliveryId}/status")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public Delivery updateDeliveryStatus(
            @PathVariable String deliveryId,
            @Valid @RequestBody UpdateDeliveryStatusRequest request,
            Authentication authentication) {
        return deliveryService.updateDeliveryStatus(
                deliveryId, request.getStatus(), authentication.getName());
    }
}
