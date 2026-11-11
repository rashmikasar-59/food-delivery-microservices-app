package com.fooddelivery.menu.service;

import com.fooddelivery.menu.dto.CreateMenuItemRequest;
import com.fooddelivery.menu.dto.UpdateMenuItemRequest;
import com.fooddelivery.menu.entity.MenuItem;
import com.fooddelivery.menu.exception.ResourceNotFoundException;
import com.fooddelivery.menu.repository.MenuItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.access.AccessDeniedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private RestaurantClient restaurantClient;

    @InjectMocks
    private MenuItemService menuItemService;

    private CreateMenuItemRequest createRequest;
    private UpdateMenuItemRequest updateRequest;

    @BeforeEach
    void setUp() {
        createRequest = new CreateMenuItemRequest();
        createRequest.setRestaurantId("restaurant-1");
        createRequest.setName("Paneer Pizza");
        createRequest.setDescription("Fresh paneer pizza");
        createRequest.setPrice(249.0);
        createRequest.setCategory("Pizza");
        createRequest.setImageUrl("pizza.jpg");
        createRequest.setVegetarian(true);

        updateRequest = new UpdateMenuItemRequest();
        updateRequest.setName("Updated Paneer Pizza");
        updateRequest.setDescription("Updated description");
        updateRequest.setPrice(299.0);
        updateRequest.setCategory("Special Pizza");
        updateRequest.setImageUrl("updated-pizza.jpg");
        updateRequest.setVegetarian(true);
        updateRequest.setAvailable(false);
    }

    @Test
    void createMenuItemShouldSaveItemForRestaurantOwner() {
        when(restaurantClient.isRestaurantOwner("restaurant-1", "owner-1"))
                .thenReturn(true);
        when(menuItemRepository.save(any(MenuItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MenuItem result = menuItemService.createMenuItem(
                createRequest, "owner-1", false);

        assertEquals("restaurant-1", result.getRestaurantId());
        assertEquals("Paneer Pizza", result.getName());
        assertEquals(249.0, result.getPrice());
        assertEquals("Pizza", result.getCategory());
        assertTrue(result.isVegetarian());
        assertTrue(result.isAvailable());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());

        verify(menuItemRepository).save(any(MenuItem.class));
    }

    @Test
    void createMenuItemShouldRejectUnauthorizedUser() {
        when(restaurantClient.isRestaurantOwner("restaurant-1", "user-1"))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> menuItemService.createMenuItem(
                        createRequest, "user-1", false));

        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    void createMenuItemShouldAllowAdminWithoutOwnershipCheck() {
        when(menuItemRepository.save(any(MenuItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MenuItem result = menuItemService.createMenuItem(
                createRequest, "admin-1", true);

        assertEquals("Paneer Pizza", result.getName());
        assertTrue(result.isAvailable());

        verify(menuItemRepository).save(any(MenuItem.class));
        verifyNoInteractions(restaurantClient);
    }

    @Test
    void getRestaurantMenuShouldReturnRestaurantItems() {
        List<MenuItem> items = List.of(
                new MenuItem("restaurant-1", "Pizza", "Cheese pizza",
                        199.0, "Pizza", null, true),
                new MenuItem("restaurant-1", "Burger", "Veg burger",
                        149.0, "Burger", null, true)
        );

        when(menuItemRepository.findByRestaurantId("restaurant-1"))
                .thenReturn(items);

        List<MenuItem> result =
                menuItemService.getRestaurantMenu("restaurant-1");

        assertEquals(2, result.size());
        verify(menuItemRepository).findByRestaurantId("restaurant-1");
    }

    @Test
    void getMenuItemByIdShouldReturnItemWhenFound() {
        MenuItem item = new MenuItem(
                "restaurant-1", "Pizza", "Cheese pizza",
                199.0, "Pizza", null, true);

        when(menuItemRepository.findById("item-1"))
                .thenReturn(Optional.of(item));

        MenuItem result = menuItemService.getMenuItemById("item-1");

        assertEquals("Pizza", result.getName());
    }

    @Test
    void getMenuItemByIdShouldThrowWhenItemDoesNotExist() {
        when(menuItemRepository.findById("missing-item"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> menuItemService.getMenuItemById("missing-item"));
    }

    @Test
    void updateMenuItemShouldUpdateItemForRestaurantOwner() {
        MenuItem existingItem = new MenuItem(
                "restaurant-1", "Old Pizza", "Old description",
                199.0, "Pizza", "old.jpg", false);
        existingItem.setId("item-1");

        when(menuItemRepository.findById("item-1"))
                .thenReturn(Optional.of(existingItem));
        when(restaurantClient.isRestaurantOwner("restaurant-1", "owner-1"))
                .thenReturn(true);
        when(menuItemRepository.save(any(MenuItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MenuItem result = menuItemService.updateMenuItem(
                "item-1", updateRequest, "owner-1", false);

        assertEquals("Updated Paneer Pizza", result.getName());
        assertEquals("Updated description", result.getDescription());
        assertEquals(299.0, result.getPrice());
        assertEquals("Special Pizza", result.getCategory());
        assertEquals("updated-pizza.jpg", result.getImageUrl());
        assertTrue(result.isVegetarian());
        assertFalse(result.isAvailable());
        assertNotNull(result.getUpdatedAt());

        verify(menuItemRepository).save(existingItem);
    }

    @Test
    void updateMenuItemShouldRejectUnauthorizedUser() {
        MenuItem existingItem = new MenuItem(
                "restaurant-1", "Pizza", "Description",
                199.0, "Pizza", null, true);

        when(menuItemRepository.findById("item-1"))
                .thenReturn(Optional.of(existingItem));
        when(restaurantClient.isRestaurantOwner("restaurant-1", "user-1"))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> menuItemService.updateMenuItem(
                        "item-1", updateRequest, "user-1", false));

        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    void updateMenuItemShouldAllowAdminWithoutOwnershipCheck() {
        MenuItem existingItem = new MenuItem(
                "restaurant-1", "Pizza", "Description",
                199.0, "Pizza", null, true);

        when(menuItemRepository.findById("item-1"))
                .thenReturn(Optional.of(existingItem));
        when(menuItemRepository.save(any(MenuItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MenuItem result = menuItemService.updateMenuItem(
                "item-1", updateRequest, "admin-1", true);

        assertEquals("Updated Paneer Pizza", result.getName());
        verify(menuItemRepository).save(existingItem);
        verifyNoInteractions(restaurantClient);
    }

    @Test
    void deleteMenuItemShouldDeleteItemForRestaurantOwner() {
        MenuItem item = new MenuItem(
                "restaurant-1", "Pizza", "Description",
                199.0, "Pizza", null, true);

        when(menuItemRepository.findById("item-1"))
                .thenReturn(Optional.of(item));
        when(restaurantClient.isRestaurantOwner("restaurant-1", "owner-1"))
                .thenReturn(true);

        assertDoesNotThrow(() ->
                menuItemService.deleteMenuItem("item-1", "owner-1", false));

        verify(menuItemRepository).delete(item);
    }

    @Test
    void deleteMenuItemShouldRejectUnauthorizedUser() {
        MenuItem item = new MenuItem(
                "restaurant-1", "Pizza", "Description",
                199.0, "Pizza", null, true);

        when(menuItemRepository.findById("item-1"))
                .thenReturn(Optional.of(item));
        when(restaurantClient.isRestaurantOwner("restaurant-1", "user-1"))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> menuItemService.deleteMenuItem(
                        "item-1", "user-1", false));

        verify(menuItemRepository, never()).delete(any(MenuItem.class));
    }

    @Test
    void deleteMenuItemShouldAllowAdminWithoutOwnershipCheck() {
        MenuItem item = new MenuItem(
                "restaurant-1", "Pizza", "Description",
                199.0, "Pizza", null, true);

        when(menuItemRepository.findById("item-1"))
                .thenReturn(Optional.of(item));

        assertDoesNotThrow(() ->
                menuItemService.deleteMenuItem("item-1", "admin-1", true));

        verify(menuItemRepository).delete(item);
        verifyNoInteractions(restaurantClient);
    }
}
