package com.fooddelivery.order.repository;

import com.fooddelivery.order.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findByCustomerId(String customerId);

    List<Order> findByRestaurantId(String restaurantId);

    List<Order> findByCustomerIdAndStatus(
            String customerId,
            com.fooddelivery.order.model.OrderStatus status
    );
}
