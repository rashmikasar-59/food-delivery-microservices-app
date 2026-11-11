package com.fooddelivery.payment.service;

import com.fooddelivery.payment.dto.CreatePaymentRequest;
import com.fooddelivery.payment.model.Payment;
import com.fooddelivery.payment.model.PaymentStatus;
import com.fooddelivery.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private CreatePaymentRequest request;

    @BeforeEach
    void setUp() {
        request = new CreatePaymentRequest();
        request.setOrderId("order-101");
        request.setAmount(499.50);
        request.setPaymentMethod("UPI");
    }

    @Test
    void createPaymentShouldSavePendingPayment() {
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.createPayment("customer-101", request);

        assertEquals("order-101", result.getOrderId());
        assertEquals("customer-101", result.getCustomerId());
        assertEquals(499.50, result.getAmount());
        assertEquals("UPI", result.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertNotNull(result.getTransactionId());
        assertFalse(result.getTransactionId().isBlank());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPaymentShouldGenerateUniqueTransactionIds() {
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment first = paymentService.createPayment("customer-101", request);
        Payment second = paymentService.createPayment("customer-101", request);

        assertNotEquals(first.getTransactionId(), second.getTransactionId());
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void getPaymentsByCustomerShouldReturnCustomerPayments() {
        List<Payment> payments = List.of(new Payment(), new Payment());
        when(paymentRepository.findByCustomerId("customer-101"))
                .thenReturn(payments);

        List<Payment> result = paymentService.getPaymentsByCustomer("customer-101");

        assertSame(payments, result);
        verify(paymentRepository).findByCustomerId("customer-101");
    }

    @Test
    void getPaymentByIdShouldReturnPaymentForOwner() {
        Payment payment = new Payment();
        payment.setId("payment-101");
        payment.setCustomerId("customer-101");

        when(paymentRepository.findById("payment-101"))
                .thenReturn(Optional.of(payment));

        Payment result = paymentService.getPaymentById("payment-101", "customer-101");

        assertSame(payment, result);
        verify(paymentRepository).findById("payment-101");
    }

    @Test
    void getPaymentByIdShouldThrowWhenPaymentDoesNotExist() {
        when(paymentRepository.findById("missing-payment"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> paymentService.getPaymentById("missing-payment", "customer-101")
        );

        assertEquals("Payment not found", exception.getMessage());
        verify(paymentRepository).findById("missing-payment");
    }

    @Test
    void getPaymentByIdShouldRejectAnotherCustomer() {
        Payment payment = new Payment();
        payment.setId("payment-101");
        payment.setCustomerId("customer-101");

        when(paymentRepository.findById("payment-101"))
                .thenReturn(Optional.of(payment));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> paymentService.getPaymentById("payment-101", "customer-202")
        );

        assertEquals("Access denied", exception.getMessage());
        verify(paymentRepository).findById("payment-101");
    }
}
