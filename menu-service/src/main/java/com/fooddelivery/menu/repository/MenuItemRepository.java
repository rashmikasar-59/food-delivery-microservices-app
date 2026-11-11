package com.fooddelivery.menu.repository;

import com.fooddelivery.menu.entity.MenuItem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MenuItemRepository extends MongoRepository<MenuItem, String> {

    List<MenuItem> findByRestaurantId(String restaurantId);

    List<MenuItem> findByRestaurantIdAndCategory(
            String restaurantId,
            String category
    );

    List<MenuItem> findByRestaurantIdAndAvailableTrue(String restaurantId);

    boolean existsByIdAndRestaurantId(String id, String restaurantId);

    void deleteByRestaurantId(String restaurantId);
}
