package com.restaurantservice.dto;


import java.util.List;

import com.restaurantservice.model.Address;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantRequest {
    @NotBlank(message = "Restaurant name is required")
    @Size(max = 120, message = "Restaurant name must be at most 120 characters")
    private String name;
    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;
    private Address address;
    private List<Long> contactInfo;
    @DecimalMin(value = "0.0", message = "Rating must be at least 0")
    @DecimalMax(value = "5.0", message = "Rating must be at most 5")
    private Double rating;
}
