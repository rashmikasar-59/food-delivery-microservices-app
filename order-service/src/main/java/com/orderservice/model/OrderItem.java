package com.orderservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document
public class OrderItem {
    private String name;
    private BigDecimal price;
    private Integer quantity;
    private String foodItemId;
}
