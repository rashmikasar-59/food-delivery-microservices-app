package com.restaurantservice.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.restaurantservice.dto.RestaurantDto;
import com.restaurantservice.dto.RestaurantRequest;
import com.restaurantservice.dto.RestaurantResponse;
import com.restaurantservice.service.RestaurantService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/restaurant")
@RequiredArgsConstructor
public class RestaurantController {
    private final RestaurantService restaurantService;
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String addRestaurant(@Valid @RequestBody RestaurantRequest request,
                                @RequestHeader("loggedInUser") String username,
                                @RequestHeader("loggedInRole") String role) {
        if (!"RESTAURANT_OWNER".equals(role) && !"ADMIN".equals(role)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only restaurant owners or admins can create restaurants");
        }
        return restaurantService.addRestaurant(request, username, role);
    }
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<RestaurantDto> getAllRestaurants() {
        return restaurantService.getAllRestaurants();
    }


    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public Page<RestaurantDto> searchRestaurants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Double minRating) {
        return restaurantService.searchRestaurants(page, size, sortBy, direction, name, minRating);
    }

    @GetMapping("/internal/{id}/owner")
    public java.util.Map<String, String> getRestaurantOwner(@PathVariable String id,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String internalKey) {
        String expected = System.getenv("INTERNAL_SERVICE_KEY");
        if (expected == null || expected.isBlank() || !expected.equals(internalKey)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Internal service authentication required");
        }
        return java.util.Map.of("username", restaurantService.getRestaurantOwnerUsername(id));
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public RestaurantResponse getRestaurant(@PathVariable("id") String id) {
        return restaurantService.getRestaurant(id);
    }
}
