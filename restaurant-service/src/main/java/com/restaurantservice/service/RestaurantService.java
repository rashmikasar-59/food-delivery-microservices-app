package com.restaurantservice.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.restaurantservice.dto.RestaurantDto;
import com.restaurantservice.dto.RestaurantRequest;
import com.restaurantservice.dto.RestaurantResponse;
import com.restaurantservice.model.FoodItem;
import com.restaurantservice.model.OwnerInfo;
import com.restaurantservice.model.Restaurant;
import com.restaurantservice.repository.FoodItemRepository;
import com.restaurantservice.repository.RestaurantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantService {
    @Autowired
    private RestaurantRepository restaurantRepository;
    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private WebClient.Builder webClientBuilder;

    public String addRestaurant(RestaurantRequest request, String username, String role) {
        if (!"RESTAURANT_OWNER".equals(role) && !"ADMIN".equals(role)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Only restaurant owners or admins can create restaurants");
        }

        Restaurant restaurant = Restaurant.builder()
                .name(request.getName())
                .description(request.getDescription())
                .contactInfo(request.getContactInfo())
                .address(request.getAddress())
                .rating(request.getRating())
                .owner(OwnerInfo.builder()
                        .username(username)
                        .build())
                .build();
        restaurantRepository.save(restaurant);
        return "Restaurant with id: " + restaurant.getId() + " added successfully";
    }

    public String getRestaurantOwnerUsername(String id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Restaurant not found"));
        if (restaurant.getOwner() == null || restaurant.getOwner().getUsername() == null) {
            throw new IllegalStateException("Restaurant owner is not configured");
        }
        return restaurant.getOwner().getUsername();
    }

    public RestaurantResponse getRestaurant(String id) {
        Optional<Restaurant> restaurant = restaurantRepository.findById(id);
        RestaurantResponse restaurantResponse = new RestaurantResponse();
        if(restaurant.isPresent()) {
            return RestaurantResponse.builder()
                    .restaurantDto(getRestaurantDto(restaurant.get()))
                    .responseCode(200)
                    .msg("Success")
                    .build();
        }
        return RestaurantResponse.builder()
                .responseCode(404)
                .msg("Restaurant not found with this id")
                .build();
    }

    public List<RestaurantDto> getAllRestaurants() {
        List<Restaurant> restaurants = restaurantRepository.findAll();
        return restaurants.stream()
                .map(this::getRestaurantDto).toList();
    }

    /** Paginated, sortable restaurant search. Supported filters: name (case-insensitive) and minRating. */
    public Page<RestaurantDto> searchRestaurants(int page, int size, String sortBy, String direction,
                                                  String name, Double minRating) {
        if (page < 0) throw new IllegalArgumentException("page must be zero or greater");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size must be between 1 and 100");
        List<String> allowedSortFields = List.of("name", "rating", "description");
        if (!allowedSortFields.contains(sortBy)) throw new IllegalArgumentException("Unsupported sort field: " + sortBy);
        Sort.Direction sortDirection;
        try { sortDirection = Sort.Direction.fromString(direction); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("direction must be asc or desc"); }
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Query query = new Query().with(pageable);
        Query countQuery = new Query();
        if (name != null && !name.isBlank()) {
            Criteria nameCriteria = Criteria.where("name").regex(java.util.regex.Pattern.quote(name.trim()), "i");
            query.addCriteria(nameCriteria);
            countQuery.addCriteria(Criteria.where("name").regex(java.util.regex.Pattern.quote(name.trim()), "i"));
        }
        if (minRating != null) {
            if (minRating < 0 || minRating > 5) throw new IllegalArgumentException("minRating must be between 0 and 5");
            query.addCriteria(Criteria.where("rating").gte(minRating));
            countQuery.addCriteria(Criteria.where("rating").gte(minRating));
        }
        List<Restaurant> rows = mongoTemplate.find(query, Restaurant.class);
        long total = mongoTemplate.count(countQuery, Restaurant.class);
        return new PageImpl<>(rows.stream().map(this::getRestaurantDto).toList(), pageable, total);
    }

    private RestaurantDto getRestaurantDto(Restaurant restaurant) {
        RestaurantDto dto = RestaurantDto.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .description(restaurant.getDescription())
                .address(restaurant.getAddress())
                .contactInfo(restaurant.getContactInfo())
                .rating(restaurant.getRating())
                .build();
        List<FoodItem> foodItems = foodItemRepository
                .findByRestaurantId(restaurant.getId());
        dto.setFoodItems(foodItems);
        return dto;
    }
}
