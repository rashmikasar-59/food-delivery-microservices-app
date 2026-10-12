package com.restaurantservice.repository;


import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.restaurantservice.model.FoodItem;

public interface FoodItemRepository extends MongoRepository<FoodItem, String> {
    List<FoodItem> findByRestaurantId(String restaurantId);
}
