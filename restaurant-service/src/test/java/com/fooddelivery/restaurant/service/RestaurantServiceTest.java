package com.fooddelivery.restaurant.service;

import com.fooddelivery.restaurant.dto.CreateRestaurantRequest;
import com.fooddelivery.restaurant.dto.UpdateRestaurantRequest;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.exception.DuplicateResourceException;
import com.fooddelivery.restaurant.exception.ResourceNotFoundException;
import com.fooddelivery.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    RestaurantRepository repository;

    @InjectMocks
    RestaurantService service;

    private CreateRestaurantRequest createRequest() {
        CreateRestaurantRequest request = new CreateRestaurantRequest();
        request.setName("Test Restaurant");
        request.setDescription("Test description");
        request.setEmail("restaurant@example.com");
        request.setPhone("9876543210");
        request.setAddress("Main Road");
        request.setCity("Pune");
        request.setCuisine("Indian");
        return request;
    }

    private UpdateRestaurantRequest updateRequest() {
        UpdateRestaurantRequest request = new UpdateRestaurantRequest();
        request.setName("Updated Restaurant");
        request.setDescription("Updated description");
        request.setPhone("9876543211");
        request.setAddress("New Road");
        request.setCity("Mumbai");
        request.setCuisine("Chinese");
        return request;
    }

    private Restaurant existingRestaurant() {
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
    void createRestaurantShouldSaveRestaurantForOwner() {
        CreateRestaurantRequest request = createRequest();
        when(repository.existsByEmail(request.getEmail())).thenReturn(false);
        when(repository.save(any(Restaurant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Restaurant result = service.createRestaurant(request, "owner-1");

        assertEquals("owner-1", result.getOwnerId());
        assertEquals("Test Restaurant", result.getName());
        assertEquals("restaurant@example.com", result.getEmail());
        assertTrue(result.isActive());

        verify(repository).save(any(Restaurant.class));
    }

    @Test
    void createRestaurantShouldRejectDuplicateEmail() {
        CreateRestaurantRequest request = createRequest();
        when(repository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> service.createRestaurant(request, "owner-1"));

        verify(repository, never()).save(any(Restaurant.class));
    }

    @Test
    void getRestaurantByIdShouldReturnRestaurant() {
        Restaurant restaurant = existingRestaurant();
        when(repository.findById("restaurant-1"))
                .thenReturn(Optional.of(restaurant));

        Restaurant result = service.getRestaurantById("restaurant-1");

        assertEquals("Test Restaurant", result.getName());
    }

    @Test
    void getRestaurantByIdShouldThrowWhenMissing() {
        when(repository.findById("missing"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getRestaurantById("missing"));
    }

    @Test
    void updateRestaurantShouldAllowOwner() {
        Restaurant restaurant = existingRestaurant();
        when(repository.findById("restaurant-1"))
                .thenReturn(Optional.of(restaurant));
        when(repository.save(restaurant)).thenReturn(restaurant);

        Restaurant result = service.updateRestaurant(
                "restaurant-1", updateRequest(), "owner-1", false);

        assertEquals("Updated Restaurant", result.getName());
        assertEquals("Mumbai", result.getCity());
        assertEquals("Chinese", result.getCuisine());
        assertNotNull(result.getUpdatedAt());

        verify(repository).save(restaurant);
    }

    @Test
    void updateRestaurantShouldRejectDifferentOwner() {
        Restaurant restaurant = existingRestaurant();
        when(repository.findById("restaurant-1"))
                .thenReturn(Optional.of(restaurant));

        assertThrows(AccessDeniedException.class,
                () -> service.updateRestaurant(
                        "restaurant-1", updateRequest(), "other-user", false));

        verify(repository, never()).save(any(Restaurant.class));
    }

    @Test
    void updateRestaurantShouldAllowAdmin() {
        Restaurant restaurant = existingRestaurant();
        when(repository.findById("restaurant-1"))
                .thenReturn(Optional.of(restaurant));
        when(repository.save(restaurant)).thenReturn(restaurant);

        Restaurant result = service.updateRestaurant(
                "restaurant-1", updateRequest(), "admin-1", true);

        assertEquals("Updated Restaurant", result.getName());
        verify(repository).save(restaurant);
    }

    @Test
    void deleteRestaurantShouldAllowOwner() {
        Restaurant restaurant = existingRestaurant();
        when(repository.findById("restaurant-1"))
                .thenReturn(Optional.of(restaurant));

        assertDoesNotThrow(() ->
                service.deleteRestaurant("restaurant-1", "owner-1", false));

        verify(repository).delete(restaurant);
    }

    @Test
    void deleteRestaurantShouldRejectDifferentOwner() {
        Restaurant restaurant = existingRestaurant();
        when(repository.findById("restaurant-1"))
                .thenReturn(Optional.of(restaurant));

        assertThrows(AccessDeniedException.class,
                () -> service.deleteRestaurant(
                        "restaurant-1", "other-user", false));

        verify(repository, never()).delete(any(Restaurant.class));
    }

    @Test
    void getAllRestaurantsShouldReturnSortedPage() {
        Restaurant restaurant = existingRestaurant();
        Page<Restaurant> expected =
                new PageImpl<>(List.of(restaurant));

        PageRequest pageable = PageRequest.of(
                0, 10, Sort.by(Sort.Direction.ASC, "name"));

        when(repository.findAll(pageable)).thenReturn(expected);

        Page<Restaurant> result =
                service.getAllRestaurants(0, 10, "name", "ASC");

        assertEquals(1, result.getTotalElements());
        assertEquals("Test Restaurant", result.getContent().get(0).getName());
        verify(repository).findAll(pageable);
    }

    @Test
    void getAllRestaurantsShouldRejectInvalidPaginationAndSorting() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getAllRestaurants(-1, 10, "name", "ASC"));

        assertThrows(IllegalArgumentException.class,
                () -> service.getAllRestaurants(0, 51, "name", "ASC"));

        assertThrows(IllegalArgumentException.class,
                () -> service.getAllRestaurants(0, 10, "password", "ASC"));

        assertThrows(IllegalArgumentException.class,
                () -> service.getAllRestaurants(0, 10, "name", "SIDEWAYS"));

        verifyNoInteractions(repository);
    }
}
