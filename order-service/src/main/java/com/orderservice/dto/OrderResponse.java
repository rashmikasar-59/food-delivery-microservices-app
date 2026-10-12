package com.orderservice.dto;


import java.math.BigDecimal;
import java.util.List;

import com.orderservice.model.Address;
import com.orderservice.model.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private String id;
    private Long orderNumber;
    private String restaurantName;
    private String userName;
    private List<OrderItemDto> orderItems;
    private BigDecimal totalAmount;
    private Long orderTime;
    private OrderStatus orderStatus;
    private Long expectedDeliveryTime;
    private Address address;
    private String paymentId;
}
