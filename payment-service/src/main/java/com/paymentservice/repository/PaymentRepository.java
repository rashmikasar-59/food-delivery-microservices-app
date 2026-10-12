package com.paymentservice.repository;


import org.springframework.data.mongodb.repository.MongoRepository;

import com.paymentservice.model.Payment;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    boolean existsByOrderId(String orderId);
}
