package com.paymentservice.controller;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.paymentservice.model.Payment;
import com.paymentservice.service.PaymentService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String pay(@Valid @RequestBody Payment payment,
                      @RequestHeader("loggedInRole") String role,
                      @RequestHeader("loggedInUser") String username,
                      @RequestHeader("Authorization") String authorization) {
        if (!"CUSTOMER".equals(role)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only customers can initiate payments");
        }
        return paymentService.processPayment(payment, username, role, authorization);
    }
}
