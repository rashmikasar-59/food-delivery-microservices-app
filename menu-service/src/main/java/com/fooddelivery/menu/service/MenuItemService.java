package com.fooddelivery.menu.service;

import com.fooddelivery.menu.dto.CreateMenuItemRequest;
import com.fooddelivery.menu.dto.UpdateMenuItemRequest;
import com.fooddelivery.menu.entity.MenuItem;
import com.fooddelivery.menu.repository.MenuItemRepository;
import org.springframework.security.access.AccessDeniedException;
import com.fooddelivery.menu.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantClient restaurantClient;

    public MenuItemService(
            MenuItemRepository menuItemRepository,
            RestaurantClient restaurantClient) {
        this.menuItemRepository = menuItemRepository;
        this.restaurantClient = restaurantClient;
    }

    public MenuItem createMenuItem(
            CreateMenuItemRequest request,
            String username,
            boolean isAdmin) {

        verifyRestaurantOwnership(
                request.getRestaurantId(), username, isAdmin);

        MenuItem item = new MenuItem();

        item.setRestaurantId(request.getRestaurantId());
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setCategory(request.getCategory());
        item.setImageUrl(request.getImageUrl());
        item.setVegetarian(request.isVegetarian());
        item.setAvailable(true);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());

        return menuItemRepository.save(item);
    }

    public List<MenuItem> getRestaurantMenu(String restaurantId) {
        return menuItemRepository.findByRestaurantId(restaurantId);
    }

    public MenuItem getMenuItemById(String id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Menu item not found: " + id));
    }

    public MenuItem updateMenuItem(
            String id,
            UpdateMenuItemRequest request,
            String username,
            boolean isAdmin) {

        MenuItem item = getMenuItemById(id);

        verifyRestaurantOwnership(
                item.getRestaurantId(), username, isAdmin);

        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setCategory(request.getCategory());
        item.setImageUrl(request.getImageUrl());
        item.setVegetarian(request.isVegetarian());
        item.setAvailable(request.isAvailable());
        item.setUpdatedAt(LocalDateTime.now());

        return menuItemRepository.save(item);
    }

    public void deleteMenuItem(
            String id,
            String username,
            boolean isAdmin) {

        MenuItem item = getMenuItemById(id);

        verifyRestaurantOwnership(
                item.getRestaurantId(), username, isAdmin);

        menuItemRepository.delete(item);
    }

    private void verifyRestaurantOwnership(
            String restaurantId,
            String username,
            boolean isAdmin) {

        if (!isAdmin &&
                !restaurantClient.isRestaurantOwner(restaurantId, username)) {
            throw new AccessDeniedException(
                    "You are not authorized to manage this restaurant's menu");
        }
    }
}
