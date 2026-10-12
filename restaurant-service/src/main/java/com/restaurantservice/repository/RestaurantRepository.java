package com.restaurantservice.repository;


import org.springframework.data.mongodb.repository.MongoRepository;

import com.restaurantservice.model.Restaurant;

public interface RestaurantRepository extends MongoRepository<Restaurant, String> {
}
