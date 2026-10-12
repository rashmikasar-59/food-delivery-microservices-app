package com.restaurantservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.restaurantservice.dto.FoodItemDto;
import com.restaurantservice.service.FoodItemService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/fooditem")
@RequiredArgsConstructor
public class FoodItemController {
    @Autowired
    private FoodItemService foodItemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String addFoodItem(@RequestBody FoodItemDto foodItemDto,
                              @RequestHeader("loggedInUser") String username,
                              @RequestHeader("loggedInRole") String role) {
        if (!"RESTAURANT_OWNER".equals(role) && !"ADMIN".equals(role)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only restaurant owners or admins can add food items");
        }
        return foodItemService.addFoodItem(foodItemDto, username, role);
    }

    @GetMapping("/{restaurantId}")
    @ResponseStatus(HttpStatus.OK)
    public List<FoodItemDto> getAllFoodItems(@PathVariable("restaurantId") String restaurantId) {
        return foodItemService.getAllFoodItems(restaurantId);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public String updateFoodItem(@RequestBody FoodItemDto foodItemDto,
                                 @RequestHeader("loggedInUser") String username,
                                 @RequestHeader("loggedInRole") String role) {
        if (!"RESTAURANT_OWNER".equals(role) && !"ADMIN".equals(role)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only restaurant owners or admins can update food items");
        }
        return foodItemService.updateFoodItem(foodItemDto, username, role);
    }

    @PutMapping("/quantity")
    @ResponseStatus(HttpStatus.OK)
    public void updateFoodItemQuantity(@RequestParam List<String> foodItemIds, @RequestParam List<Integer> orderQuantities,
                                       @RequestHeader(value = "X-Internal-Service-Key", required = false) String internalKey) {
        String expected = System.getenv("INTERNAL_SERVICE_KEY");
        if (expected == null || expected.isBlank() || !expected.equals(internalKey)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Internal service authentication required");
        }
        foodItemService.updateFoodItemQuantity(foodItemIds, orderQuantities);
    }

}
