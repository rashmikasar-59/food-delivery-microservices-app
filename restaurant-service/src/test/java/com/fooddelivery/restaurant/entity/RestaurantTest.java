package com.fooddelivery.restaurant.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RestaurantTest {

    @Test
    void defaultConstructorCreatesRestaurant() {
        Restaurant restaurant = new Restaurant();

        assertNotNull(restaurant);
        assertNull(restaurant.getId());
        assertFalse(restaurant.isActive());
    }

    @Test
    void parameterizedConstructorInitializesFields() {
        Restaurant restaurant = new Restaurant(
                "owner-101",
                "restaurant@example.com",
                "Test Restaurant",
                "Test description",
                "9876543210",
                "Main Road",
                "Pune",
                "Indian"
        );

        assertEquals("owner-101", restaurant.getOwnerId());
        assertEquals("restaurant@example.com", restaurant.getEmail());
        assertEquals("Test Restaurant", restaurant.getName());
        assertEquals("Test description", restaurant.getDescription());
        assertEquals("9876543210", restaurant.getPhone());
        assertEquals("Main Road", restaurant.getAddress());
        assertEquals("Pune", restaurant.getCity());
        assertEquals("Indian", restaurant.getCuisine());
        assertTrue(restaurant.isActive());
    }

    @Test
    void parameterizedConstructorInitializesTimestamps() {
        LocalDateTime before = LocalDateTime.now();

        Restaurant restaurant = new Restaurant(
                "owner-101",
                "restaurant@example.com",
                "Test Restaurant",
                "Description",
                "9876543210",
                "Main Road",
                "Pune",
                "Indian"
        );

        LocalDateTime after = LocalDateTime.now();

        assertNotNull(restaurant.getCreatedAt());
        assertNotNull(restaurant.getUpdatedAt());
        assertFalse(restaurant.getCreatedAt().isBefore(before));
        assertFalse(restaurant.getCreatedAt().isAfter(after));
        assertFalse(restaurant.getUpdatedAt().isBefore(before));
        assertFalse(restaurant.getUpdatedAt().isAfter(after));
    }

    @Test
    void gettersAndSettersWorkCorrectly() {
        Restaurant restaurant = new Restaurant();

        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        restaurant.setId("restaurant-101");
        restaurant.setOwnerId("owner-101");
        restaurant.setEmail("test@example.com");
        restaurant.setName("Updated Restaurant");
        restaurant.setDescription("Updated description");
        restaurant.setPhone("9876543210");
        restaurant.setAddress("Updated Address");
        restaurant.setCity("Mumbai");
        restaurant.setCuisine("Chinese");
        restaurant.setActive(true);
        restaurant.setCreatedAt(createdAt);
        restaurant.setUpdatedAt(updatedAt);

        assertEquals("restaurant-101", restaurant.getId());
        assertEquals("owner-101", restaurant.getOwnerId());
        assertEquals("test@example.com", restaurant.getEmail());
        assertEquals("Updated Restaurant", restaurant.getName());
        assertEquals("Updated description", restaurant.getDescription());
        assertEquals("9876543210", restaurant.getPhone());
        assertEquals("Updated Address", restaurant.getAddress());
        assertEquals("Mumbai", restaurant.getCity());
        assertEquals("Chinese", restaurant.getCuisine());
        assertTrue(restaurant.isActive());
        assertEquals(createdAt, restaurant.getCreatedAt());
        assertEquals(updatedAt, restaurant.getUpdatedAt());
    }

    @Test
    void activeStatusCanBeChanged() {
        Restaurant restaurant = new Restaurant();

        restaurant.setActive(true);
        assertTrue(restaurant.isActive());

        restaurant.setActive(false);
        assertFalse(restaurant.isActive());
    }
}
