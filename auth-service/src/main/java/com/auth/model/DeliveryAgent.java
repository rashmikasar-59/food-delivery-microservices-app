package com.auth.model;

import lombok.Builder;

@Builder
public class DeliveryAgent {
    private String name;
    private Address address;
    private Long phoneNumber;
}
