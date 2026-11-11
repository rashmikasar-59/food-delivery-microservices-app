package com.fooddelivery.menu.controller;

import com.fooddelivery.menu.dto.CreateMenuItemRequest;
import com.fooddelivery.menu.dto.UpdateMenuItemRequest;
import com.fooddelivery.menu.entity.MenuItem;
import com.fooddelivery.menu.service.MenuItemService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menu-items")
public class MenuItemController {

    private final MenuItemService menuItemService;

    public MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MenuItem> createMenuItem(
            @Valid @RequestBody CreateMenuItemRequest request,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        MenuItem item = menuItemService.createMenuItem(
                request, authentication.getName(), isAdmin);

        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<MenuItem>> getRestaurantMenu(
            @PathVariable String restaurantId) {

        return ResponseEntity.ok(
                menuItemService.getRestaurantMenu(restaurantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuItem> getMenuItem(@PathVariable String id) {
        return ResponseEntity.ok(menuItemService.getMenuItemById(id));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MenuItem> updateMenuItem(
            @PathVariable String id,
            @Valid @RequestBody UpdateMenuItemRequest request,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        MenuItem item = menuItemService.updateMenuItem(
                id, request, authentication.getName(), isAdmin);

        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteMenuItem(
            @PathVariable String id,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        menuItemService.deleteMenuItem(
                id, authentication.getName(), isAdmin);

        return ResponseEntity.noContent().build();
    }
}
