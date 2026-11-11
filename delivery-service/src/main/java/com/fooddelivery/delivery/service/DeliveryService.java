package com.fooddelivery.delivery.service;

import com.fooddelivery.delivery.dto.CreateDeliveryRequest;
import com.fooddelivery.delivery.model.Delivery;
import com.fooddelivery.delivery.model.DeliveryStatus;
import com.fooddelivery.delivery.repository.DeliveryRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    public DeliveryService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    public Delivery createDelivery(CreateDeliveryRequest request) {

        if (deliveryRepository.findByOrderId(request.getOrderId()).isPresent()) {
            throw new IllegalArgumentException(
                    "Delivery already exists for this order");
        }

        LocalDateTime now = LocalDateTime.now();

        Delivery delivery = new Delivery();
        delivery.setOrderId(request.getOrderId());
        delivery.setCustomerId(request.getCustomerId().trim().toLowerCase());
        delivery.setDeliveryPersonId(
                request.getDeliveryPersonId().trim().toLowerCase());
        delivery.setDeliveryAddress(request.getDeliveryAddress());
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setAssignedAt(now);
        delivery.setCreatedAt(now);
        delivery.setUpdatedAt(now);

        return deliveryRepository.save(delivery);
    }

    public List<Delivery> getCustomerDeliveries(String customerId) {
        return deliveryRepository.findByCustomerId(customerId);
    }

    public List<Delivery> getAssignedDeliveries(String deliveryPersonId) {
        return deliveryRepository.findByDeliveryPersonId(deliveryPersonId);
    }

    public Delivery getDeliveryById(String deliveryId, String username) {

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Delivery not found"));

        boolean isCustomer = username.equalsIgnoreCase(
                delivery.getCustomerId());
        boolean isDeliveryPerson = username.equalsIgnoreCase(
                delivery.getDeliveryPersonId());

        if (!isCustomer && !isDeliveryPerson) {
            throw new AccessDeniedException(
                    "You are not authorized to view this delivery");
        }

        return delivery;
    }

    public Delivery updateDeliveryStatus(
            String deliveryId,
            DeliveryStatus newStatus,
            String username) {

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Delivery not found"));

        if (!username.equalsIgnoreCase(delivery.getDeliveryPersonId())) {
            throw new AccessDeniedException(
                    "Only the assigned delivery person can update status");
        }

        DeliveryStatus currentStatus = delivery.getStatus();

        boolean validTransition =
                (currentStatus == DeliveryStatus.ASSIGNED
                        && (newStatus == DeliveryStatus.PICKED_UP
                        || newStatus == DeliveryStatus.FAILED))
                || (currentStatus == DeliveryStatus.PICKED_UP
                        && (newStatus == DeliveryStatus.IN_TRANSIT
                        || newStatus == DeliveryStatus.FAILED))
                || (currentStatus == DeliveryStatus.IN_TRANSIT
                        && (newStatus == DeliveryStatus.DELIVERED
                        || newStatus == DeliveryStatus.FAILED));

        if (!validTransition) {
            throw new IllegalArgumentException(
                    "Invalid delivery status transition: "
                            + currentStatus + " to " + newStatus);
        }

        LocalDateTime now = LocalDateTime.now();

        delivery.setStatus(newStatus);
        delivery.setUpdatedAt(now);

        if (newStatus == DeliveryStatus.PICKED_UP) {
            delivery.setPickedUpAt(now);
        }

        if (newStatus == DeliveryStatus.DELIVERED) {
            delivery.setDeliveredAt(now);
        }

        return deliveryRepository.save(delivery);
    }
}
