package com.orderservice.repository;


import org.springframework.data.mongodb.repository.MongoRepository;

import com.orderservice.model.Order;

public interface OrderRepository extends MongoRepository<Order, String> {
}
