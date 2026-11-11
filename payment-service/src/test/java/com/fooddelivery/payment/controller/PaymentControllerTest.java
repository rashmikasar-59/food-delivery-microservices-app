package com.fooddelivery.payment.controller;

import com.fooddelivery.payment.dto.CreatePaymentRequest;
import com.fooddelivery.payment.model.Payment;
import com.fooddelivery.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentControllerTest {

    private PaymentService paymentService;
    private PaymentController paymentController;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentService.class);
        paymentController = new PaymentController(paymentService);
        authentication = new UsernamePasswordAuthenticationToken(
                "customer-123", null
        );
    }

    @Test
    void shouldCreatePayment() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("order-123");
        request.setAmount(500.0);
        request.setPaymentMethod("UPI");

        Payment payment = new Payment();
        payment.setId("payment-123");

        when(paymentService.createPayment("customer-123", request))
                .thenReturn(payment);

        ResponseEntity<Payment> response =
                paymentController.createPayment(authentication, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(payment, response.getBody());
        verify(paymentService).createPayment("customer-123", request);
    }

    @Test
    void shouldGetPaymentsForAuthenticatedCustomer() {
        List<Payment> payments = List.of(new Payment(), new Payment());

        when(paymentService.getPaymentsByCustomer("customer-123"))
                .thenReturn(payments);

        ResponseEntity<List<Payment>> response =
                paymentController.getMyPayments(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(payments, response.getBody());
        verify(paymentService).getPaymentsByCustomer("customer-123");
    }

    @Test
    void shouldGetPaymentByIdForAuthenticatedCustomer() {
        Payment payment = new Payment();
        payment.setId("payment-123");

        when(paymentService.getPaymentById("payment-123", "customer-123"))
                .thenReturn(payment);

        ResponseEntity<Payment> response =
                paymentController.getPaymentById("payment-123", authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(payment, response.getBody());
        verify(paymentService).getPaymentById("payment-123", "customer-123");
    }
}
