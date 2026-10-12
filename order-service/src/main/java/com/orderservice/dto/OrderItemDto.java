package com.orderservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class OrderItemDto {
    private String name;
    private BigDecimal price;
    private Integer quantity;
}
