package com.fooddelivery.delivery.repository;

import com.fooddelivery.delivery.model.Delivery;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends MongoRepository<Delivery, String> {

    List<Delivery> findByCustomerId(String customerId);

    List<Delivery> findByDeliveryPersonId(String deliveryPersonId);

    Optional<Delivery> findByOrderId(String orderId);
}
