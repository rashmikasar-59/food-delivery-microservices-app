package com.fooddelivery.delivery.service;

import com.fooddelivery.delivery.dto.CreateDeliveryRequest;
import com.fooddelivery.delivery.model.Delivery;
import com.fooddelivery.delivery.model.DeliveryStatus;
import com.fooddelivery.delivery.repository.DeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private DeliveryService deliveryService;

    private CreateDeliveryRequest request;

    @BeforeEach
    void setUp() {
        request = new CreateDeliveryRequest();
        request.setOrderId("order-101");
        request.setCustomerId(" Customer@Example.com ");
        request.setDeliveryPersonId(" Rider@Example.com ");
        request.setDeliveryAddress("Pune, Maharashtra");
    }

    @Test
    void createDeliveryShouldSaveAssignedDelivery() {
        when(deliveryRepository.findByOrderId("order-101"))
                .thenReturn(Optional.empty());
        when(deliveryRepository.save(any(Delivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Delivery result = deliveryService.createDelivery(request);

        assertEquals("order-101", result.getOrderId());
        assertEquals("customer@example.com", result.getCustomerId());
        assertEquals("rider@example.com", result.getDeliveryPersonId());
        assertEquals("Pune, Maharashtra", result.getDeliveryAddress());
        assertEquals(DeliveryStatus.ASSIGNED, result.getStatus());
        assertNotNull(result.getAssignedAt());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        verify(deliveryRepository).save(any(Delivery.class));
    }

    @Test
    void createDeliveryShouldRejectDuplicateOrder() {
        when(deliveryRepository.findByOrderId("order-101"))
                .thenReturn(Optional.of(new Delivery()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> deliveryService.createDelivery(request)
        );

        assertEquals("Delivery already exists for this order",
                exception.getMessage());
        verify(deliveryRepository, never()).save(any(Delivery.class));
    }

    @Test
    void getCustomerDeliveriesShouldReturnCustomerDeliveries() {
        List<Delivery> deliveries = List.of(new Delivery(), new Delivery());
        when(deliveryRepository.findByCustomerId("customer@example.com"))
                .thenReturn(deliveries);

        List<Delivery> result =
                deliveryService.getCustomerDeliveries("customer@example.com");

        assertSame(deliveries, result);
        verify(deliveryRepository).findByCustomerId("customer@example.com");
    }

    @Test
    void getAssignedDeliveriesShouldReturnRiderDeliveries() {
        List<Delivery> deliveries = List.of(new Delivery());
        when(deliveryRepository.findByDeliveryPersonId("rider@example.com"))
                .thenReturn(deliveries);

        List<Delivery> result =
                deliveryService.getAssignedDeliveries("rider@example.com");

        assertSame(deliveries, result);
        verify(deliveryRepository).findByDeliveryPersonId("rider@example.com");
    }

    @Test
    void getDeliveryByIdShouldAllowCustomer() {
        Delivery delivery = createDelivery(DeliveryStatus.ASSIGNED);
        when(deliveryRepository.findById("delivery-101"))
                .thenReturn(Optional.of(delivery));

        Delivery result = deliveryService.getDeliveryById(
                "delivery-101", "CUSTOMER@example.com");

        assertSame(delivery, result);
    }

    @Test
    void getDeliveryByIdShouldAllowAssignedDeliveryPerson() {
        Delivery delivery = createDelivery(DeliveryStatus.ASSIGNED);
        when(deliveryRepository.findById("delivery-101"))
                .thenReturn(Optional.of(delivery));

        Delivery result = deliveryService.getDeliveryById(
                "delivery-101", "RIDER@example.com");

        assertSame(delivery, result);
    }

    @Test
    void getDeliveryByIdShouldRejectUnrelatedUser() {
        Delivery delivery = createDelivery(DeliveryStatus.ASSIGNED);
        when(deliveryRepository.findById("delivery-101"))
                .thenReturn(Optional.of(delivery));

        assertThrows(AccessDeniedException.class,
                () -> deliveryService.getDeliveryById(
                        "delivery-101", "other@example.com"));
    }

    @Test
    void getDeliveryByIdShouldThrowWhenDeliveryDoesNotExist() {
        when(deliveryRepository.findById("missing"))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> deliveryService.getDeliveryById(
                        "missing", "customer@example.com")
        );

        assertEquals("Delivery not found", exception.getMessage());
    }

    @Test
    void updateDeliveryStatusShouldAllowAssignedRiderToPickUp() {
        Delivery delivery = createDelivery(DeliveryStatus.ASSIGNED);
        when(deliveryRepository.findById("delivery-101"))
                .thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Delivery result = deliveryService.updateDeliveryStatus(
                "delivery-101", DeliveryStatus.PICKED_UP,
                "rider@example.com");

        assertEquals(DeliveryStatus.PICKED_UP, result.getStatus());
        assertNotNull(result.getPickedUpAt());
        assertNotNull(result.getUpdatedAt());
        verify(deliveryRepository).save(delivery);
    }

    @Test
    void updateDeliveryStatusShouldAllowInTransitToBeDelivered() {
        Delivery delivery = createDelivery(DeliveryStatus.IN_TRANSIT);
        when(deliveryRepository.findById("delivery-101"))
                .thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Delivery result = deliveryService.updateDeliveryStatus(
                "delivery-101", DeliveryStatus.DELIVERED,
                "rider@example.com");

        assertEquals(DeliveryStatus.DELIVERED, result.getStatus());
        assertNotNull(result.getDeliveredAt());
        verify(deliveryRepository).save(delivery);
    }

    @Test
    void updateDeliveryStatusShouldRejectAnotherUser() {
        Delivery delivery = createDelivery(DeliveryStatus.ASSIGNED);
        when(deliveryRepository.findById("delivery-101"))
                .thenReturn(Optional.of(delivery));

        assertThrows(AccessDeniedException.class,
                () -> deliveryService.updateDeliveryStatus(
                        "delivery-101", DeliveryStatus.PICKED_UP,
                        "other@example.com"));

        verify(deliveryRepository, never()).save(any(Delivery.class));
    }

    @Test
    void updateDeliveryStatusShouldRejectInvalidTransition() {
        Delivery delivery = createDelivery(DeliveryStatus.ASSIGNED);
        when(deliveryRepository.findById("delivery-101"))
                .thenReturn(Optional.of(delivery));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> deliveryService.updateDeliveryStatus(
                        "delivery-101", DeliveryStatus.DELIVERED,
                        "rider@example.com")
        );

        assertTrue(exception.getMessage().contains("Invalid delivery status transition"));
        verify(deliveryRepository, never()).save(any(Delivery.class));
    }

    @Test
    void updateDeliveryStatusShouldAllowFailureFromAssigned() {
        Delivery delivery = createDelivery(DeliveryStatus.ASSIGNED);
        when(deliveryRepository.findById("delivery-101"))
                .thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Delivery result = deliveryService.updateDeliveryStatus(
                "delivery-101", DeliveryStatus.FAILED,
                "rider@example.com");

        assertEquals(DeliveryStatus.FAILED, result.getStatus());
        verify(deliveryRepository).save(delivery);
    }

    private Delivery createDelivery(DeliveryStatus status) {
        Delivery delivery = new Delivery();
        delivery.setId("delivery-101");
        delivery.setOrderId("order-101");
        delivery.setCustomerId("customer@example.com");
        delivery.setDeliveryPersonId("rider@example.com");
        delivery.setDeliveryAddress("Pune, Maharashtra");
        delivery.setStatus(status);
        return delivery;
    }
}
