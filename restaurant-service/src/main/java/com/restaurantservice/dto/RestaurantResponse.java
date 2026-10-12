package com.restaurantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RestaurantResponse {
    private RestaurantDto restaurantDto;
    private Integer responseCode;
    private String msg;
}
