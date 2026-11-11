package com.fooddelivery.payment.repository;

import com.fooddelivery.payment.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends MongoRepository<Payment, String> {

    List<Payment> findByCustomerId(String customerId);

    Optional<Payment> findByOrderId(String orderId);
}
