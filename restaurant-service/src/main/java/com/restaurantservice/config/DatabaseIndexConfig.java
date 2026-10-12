package com.restaurantservice.config;



import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

import com.restaurantservice.model.FoodItem;
import com.restaurantservice.model.Restaurant;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class DatabaseIndexConfig {
    private final MongoTemplate mongoTemplate;
    @PostConstruct public void ensureIndexes() {
        IndexOperations restaurants = mongoTemplate.indexOps(Restaurant.class);
        restaurants.ensureIndex(new Index().on("name", Sort.Direction.ASC).named("idx_restaurant_name"));
        restaurants.ensureIndex(new Index().on("rating", Sort.Direction.DESC).named("idx_restaurant_rating"));
        IndexOperations foodItems = mongoTemplate.indexOps(FoodItem.class);
        foodItems.ensureIndex(new Index().on("restaurantId", Sort.Direction.ASC).named("idx_fooditem_restaurant_id"));
        foodItems.ensureIndex(new Index().on("name", Sort.Direction.ASC).named("idx_fooditem_name"));
    }
}
