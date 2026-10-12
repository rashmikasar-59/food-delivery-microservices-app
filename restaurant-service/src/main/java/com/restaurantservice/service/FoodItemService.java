package com.restaurantservice.service;


import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.restaurantservice.dto.FoodItemDto;
import com.restaurantservice.model.FoodItem;
import com.restaurantservice.model.Restaurant;
import com.restaurantservice.repository.FoodItemRepository;
import com.restaurantservice.repository.RestaurantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FoodItemService {
    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    public String addFoodItem(FoodItemDto foodItemDto, String username, String role) {
        // check if the loggedInUser is same as the restaurant Owner or not
        Restaurant restaurant = restaurantRepository.findById(foodItemDto.getRestaurantId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Restaurant not found"));
        if (!"ADMIN".equals(role) && (restaurant.getOwner() == null
                || !restaurant.getOwner().getUsername().equalsIgnoreCase(username))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "You do not own this restaurant");
        }
        FoodItem foodItem = FoodItem.builder()
                .name(foodItemDto.getName())
                .description(foodItemDto.getDescription())
                .quantity(foodItemDto.getQuantity())
                .icon(foodItemDto.getIcon())
                .price(foodItemDto.getPrice())
                .restaurantId(foodItemDto.getRestaurantId())
                .build();
        foodItemRepository.save(foodItem);
        return "Food item added with id: " + foodItem.getId();
    }

    public List<FoodItemDto> getAllFoodItems(String restaurantId) {
        List<FoodItem> allFoodItems =  foodItemRepository.findByRestaurantId(restaurantId);
        return allFoodItems.stream().map(foodItem ->
                FoodItemDto.builder()
                        .id(foodItem.getId())
                        .name(foodItem.getName())
                        .description(foodItem.getDescription())
                        .icon(foodItem.getIcon())
                        .quantity(foodItem.getQuantity())
                        .price(foodItem.getPrice())
                        .restaurantId(foodItem.getRestaurantId())
                        .build()).toList();
    }

    public String updateFoodItem(FoodItemDto foodItemDto, String username, String role) {
        // check if the loggedInUser is same as the restaurant Owner or not
        Restaurant restaurant = restaurantRepository.findById(foodItemDto.getRestaurantId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Restaurant not found"));
        if (!"ADMIN".equals(role) && (restaurant.getOwner() == null
                || !restaurant.getOwner().getUsername().equalsIgnoreCase(username))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "You do not own this restaurant");
        }
        Optional<FoodItem> foodItem = foodItemRepository.findById(foodItemDto.getId());
        if(foodItem.isPresent()) {
            update(foodItem.get(), foodItemDto);
            return "Updated Successfully";
        }
        return "Food Item not present";
    }

    private void update(FoodItem item, FoodItemDto foodItemDto) {
        item.setName(foodItemDto.getName());
        item.setDescription(foodItemDto.getDescription());
        item.setIcon(foodItemDto.getIcon());
        item.setPrice(foodItemDto.getPrice());
        item.setQuantity(foodItemDto.getQuantity());
        foodItemRepository.save(item);
    }

    public void updateFoodItemQuantity(List<String> foodItemIds, List<Integer> orderQuantities) {
        if (foodItemIds == null || orderQuantities == null || foodItemIds.isEmpty()
                || foodItemIds.size() != orderQuantities.size()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Food item ids and quantities must have the same non-zero length");
        }
        java.util.Map<String, Integer> requested = new java.util.LinkedHashMap<>();
        for (int i = 0; i < foodItemIds.size(); i++) {
            if (foodItemIds.get(i) == null || foodItemIds.get(i).isBlank()
                    || orderQuantities.get(i) == null || orderQuantities.get(i) < 1) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Food item id and quantity must be valid");
            }
            requested.merge(foodItemIds.get(i), orderQuantities.get(i), Integer::sum);
        }
        // Validate the aggregate request before writing to avoid overselling duplicate ids.
        for (var entry : requested.entrySet()) {
            FoodItem item = foodItemRepository.findById(entry.getKey())
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.NOT_FOUND, "Food item not found"));
            if (item.getQuantity() == null || item.getQuantity() < entry.getValue()) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT, "Insufficient inventory for food item " + item.getId());
            }
        }
        for (var entry : requested.entrySet()) updateFoodItemQuantity(entry.getKey(), entry.getValue());
    }

    private void updateFoodItemQuantity(String foodItemId, Integer quantity) {
        Optional<FoodItem> foodItem = foodItemRepository.findById(foodItemId);
        if(foodItem.isPresent()) {
            FoodItem foodItem1 = foodItem.get();
            if (foodItem1.getQuantity() == null || quantity == null || quantity < 1 || foodItem1.getQuantity() < quantity) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT, "Insufficient inventory");
            }
            foodItem1.setQuantity(foodItem1.getQuantity() - quantity);
            foodItemRepository.save(foodItem1);
        }
    }

}
