package com.fooddelivery.payment.controller;

import com.fooddelivery.payment.dto.CreatePaymentRequest;
import com.fooddelivery.payment.model.Payment;
import com.fooddelivery.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Payment> createPayment(
            Authentication authentication,
            @Valid @RequestBody CreatePaymentRequest request) {

        Payment payment = paymentService.createPayment(
                authentication.getName(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @GetMapping("/customer")
    public ResponseEntity<List<Payment>> getMyPayments(
            Authentication authentication) {

        return ResponseEntity.ok(
                paymentService.getPaymentsByCustomer(authentication.getName()));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPaymentById(
            @PathVariable String paymentId,
            Authentication authentication) {

        return ResponseEntity.ok(
                paymentService.getPaymentById(
                        paymentId, authentication.getName()));
    }
}
