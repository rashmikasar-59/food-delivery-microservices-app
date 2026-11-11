package com.fooddelivery.restaurant.repository;

import com.fooddelivery.restaurant.entity.Restaurant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository
        extends MongoRepository<Restaurant, String> {

    List<Restaurant> findByOwnerId(String ownerId);

    Optional<Restaurant> findByEmail(String email);

    boolean existsByEmail(String email);
}