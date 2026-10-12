package com.paymentservice.model;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "payments")
public class Payment {
    @Id
    private String id;
    private Long timestamp;
    private Double amount;
    private CreditCardInfo creditCardInfo;
    private String orderId;
    private PaymentStatus paymentStatus;

}
