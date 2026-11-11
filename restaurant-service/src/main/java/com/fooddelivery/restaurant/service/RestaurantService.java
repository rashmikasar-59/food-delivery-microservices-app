package com.fooddelivery.restaurant.service;

import com.fooddelivery.restaurant.dto.CreateRestaurantRequest;
import com.fooddelivery.restaurant.dto.UpdateRestaurantRequest;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.exception.DuplicateResourceException;
import com.fooddelivery.restaurant.exception.ResourceNotFoundException;
import com.fooddelivery.restaurant.repository.RestaurantRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class RestaurantService {

    private final RestaurantRepository repository;

    public RestaurantService(RestaurantRepository repository) {
        this.repository = repository;
    }

    public Restaurant createRestaurant(
            CreateRestaurantRequest request,
            String ownerId) {

        if (repository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Restaurant email already exists");
        }

        Restaurant restaurant = new Restaurant(
                ownerId,
                request.getEmail(),
                request.getName(),
                request.getDescription(),
                request.getPhone(),
                request.getAddress(),
                request.getCity(),
                request.getCuisine()
        );

        return repository.save(restaurant);
    }

    public Restaurant getRestaurantById(String id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Restaurant not found: " + id));
    }

    private void verifyOwnership(
            Restaurant restaurant,
            String currentUser,
            boolean isAdmin) {

        if (!isAdmin &&
                !restaurant.getOwnerId().equals(currentUser)) {
            throw new AccessDeniedException(
                    "You are not authorized to manage this restaurant");
        }
    }

    public Restaurant updateRestaurant(
            String id,
            UpdateRestaurantRequest request,
            String currentUser,
            boolean isAdmin) {

        Restaurant restaurant = getRestaurantById(id);

        verifyOwnership(restaurant, currentUser, isAdmin);

        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setPhone(request.getPhone());
        restaurant.setAddress(request.getAddress());
        restaurant.setCity(request.getCity());
        restaurant.setCuisine(request.getCuisine());
        restaurant.setUpdatedAt(LocalDateTime.now());

        return repository.save(restaurant);
    }

    public void deleteRestaurant(
            String id,
            String currentUser,
            boolean isAdmin) {

        Restaurant restaurant = getRestaurantById(id);

        verifyOwnership(restaurant, currentUser, isAdmin);

        repository.delete(restaurant);
    }

    public Page<Restaurant> getAllRestaurants(
            int page,
            int size,
            String sortBy,
            String direction) {

        if (page < 0 || size < 1 || size > 50) {
            throw new IllegalArgumentException(
                    "Page must be >= 0 and size must be between 1 and 50");
        }

        if (!Set.of(
                "name", "city", "cuisine", "createdAt"
        ).contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sort field: " + sortBy);
        }

        Sort.Direction sortDirection =
                Sort.Direction.fromString(direction);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );

        return repository.findAll(pageable);
    }
}
