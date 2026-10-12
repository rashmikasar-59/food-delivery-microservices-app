package com.restaurantservice.dto;

import java.util.List;

import com.restaurantservice.model.Address;
import com.restaurantservice.model.FoodItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RestaurantDto {
    private String id;
    private String name;
    private String description;
    private Address address;
    private List<Long> contactInfo;
    private Double rating;
    private List<FoodItem> foodItems;
}
