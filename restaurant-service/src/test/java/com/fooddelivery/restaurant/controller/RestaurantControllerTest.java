package com.fooddelivery.restaurant.controller;

import com.fooddelivery.restaurant.dto.CreateRestaurantRequest;
import com.fooddelivery.restaurant.dto.UpdateRestaurantRequest;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.service.RestaurantService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RestaurantControllerTest {

    private final RestaurantService restaurantService =
            mock(RestaurantService.class);

    private final RestaurantController controller =
            new RestaurantController(restaurantService);

    private Authentication authentication(String username, String role) {
        return new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
    }

    private Restaurant restaurant() {
        return new Restaurant(
                "owner-1",
                "restaurant@example.com",
                "Test Restaurant",
                "Description",
                "9876543210",
                "Main Road",
                "Pune",
                "Indian"
        );
    }

    @Test
    void shouldCreateRestaurantAndReturnCreatedStatus() {
        CreateRestaurantRequest request = mock(CreateRestaurantRequest.class);
        Restaurant restaurant = restaurant();

        when(restaurantService.createRestaurant(request, "owner-1"))
                .thenReturn(restaurant);

        ResponseEntity<Restaurant> response =
                controller.createRestaurant(
                        request, authentication("owner-1", "ROLE_OWNER"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(restaurant, response.getBody());
        verify(restaurantService).createRestaurant(request, "owner-1");
    }

    @Test
    void shouldReturnAllRestaurants() {
        Page<Restaurant> page = new PageImpl<>(List.of(restaurant()));

        when(restaurantService.getAllRestaurants(0, 10, "createdAt", "desc"))
                .thenReturn(page);

        ResponseEntity<Page<Restaurant>> response =
                controller.getAllRestaurants(0, 10, "createdAt", "desc");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(page, response.getBody());
        verify(restaurantService).getAllRestaurants(
                0, 10, "createdAt", "desc");
    }

    @Test
    void shouldReturnRestaurantById() {
        Restaurant restaurant = restaurant();

        when(restaurantService.getRestaurantById("restaurant-1"))
                .thenReturn(restaurant);

        ResponseEntity<Restaurant> response =
                controller.getRestaurantById("restaurant-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(restaurant, response.getBody());
    }

    @Test
    void shouldUpdateRestaurantForOwner() {
        UpdateRestaurantRequest request = mock(UpdateRestaurantRequest.class);
        Restaurant restaurant = restaurant();

        when(restaurantService.updateRestaurant(
                "restaurant-1", request, "owner-1", false))
                .thenReturn(restaurant);

        ResponseEntity<Restaurant> response =
                controller.updateRestaurant(
                        "restaurant-1",
                        request,
                        authentication("owner-1", "ROLE_OWNER"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(restaurant, response.getBody());
        verify(restaurantService).updateRestaurant(
                "restaurant-1", request, "owner-1", false);
    }

    @Test
    void shouldUpdateRestaurantForAdmin() {
        UpdateRestaurantRequest request = mock(UpdateRestaurantRequest.class);
        Restaurant restaurant = restaurant();

        when(restaurantService.updateRestaurant(
                "restaurant-1", request, "admin-1", true))
                .thenReturn(restaurant);

        ResponseEntity<Restaurant> response =
                controller.updateRestaurant(
                        "restaurant-1",
                        request,
                        authentication("admin-1", "ROLE_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(restaurantService).updateRestaurant(
                "restaurant-1", request, "admin-1", true);
    }

    @Test
    void shouldDeleteRestaurantForOwner() {
        ResponseEntity<Void> response =
                controller.deleteRestaurant(
                        "restaurant-1",
                        authentication("owner-1", "ROLE_OWNER"));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(restaurantService).deleteRestaurant(
                "restaurant-1", "owner-1", false);
    }

    @Test
    void shouldDeleteRestaurantForAdmin() {
        ResponseEntity<Void> response =
                controller.deleteRestaurant(
                        "restaurant-1",
                        authentication("admin-1", "ROLE_ADMIN"));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(restaurantService).deleteRestaurant(
                "restaurant-1", "admin-1", true);
    }
}
